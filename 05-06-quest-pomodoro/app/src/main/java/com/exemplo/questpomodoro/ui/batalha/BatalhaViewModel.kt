package com.exemplo.questpomodoro.ui.batalha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exemplo.questpomodoro.data.repository.QuestRepository
import com.exemplo.questpomodoro.data.repository.SessaoRepository
import com.exemplo.questpomodoro.domain.Relogio
import com.exemplo.questpomodoro.domain.SessaoMaquina
import com.exemplo.questpomodoro.domain.model.Fase
import com.exemplo.questpomodoro.domain.model.Sessao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BatalhaUiState(
    val fase: Fase? = null,
    val titulo: String = "",
    val rodadaAtual: Int = 0,
    val rodadasTotais: Int = 0,
    val restanteMs: Long = 0,
    val progresso: Float = 0f,
    /** Sessão terminou (vitória ou desistência): voltar para a Taverna. */
    val encerrada: Boolean = false,
)

fun textoConfirmacaoDesistir(fase: Fase, rodadaAtual: Int, total: Int): String = when (fase) {
    Fase.FOCO, Fase.PAUSADO -> "Tem certeza? A batalha atual não vai contar."
    Fase.DESCANSO, Fase.AGUARDANDO -> "Sair da quest? Seu progresso (${rodadaAtual - 1}/$total) fica salvo."
}

private val tiquePadrao: Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(1000)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BatalhaViewModel(
    private val sessaoRepo: SessaoRepository,
    questRepo: QuestRepository,
    private val relogio: Relogio,
    tique: Flow<Unit> = tiquePadrao,
) : ViewModel() {

    private var ultimoFimSincronizado: Long? = null

    private val sessaoComTitulo: Flow<Pair<Sessao, String>?> = sessaoRepo.sessao.flatMapLatest { s ->
        if (s == null) flowOf(null) else questRepo.observarQuest(s.questId).map { q -> s to (q?.titulo ?: "") }
    }

    val uiState: StateFlow<BatalhaUiState> = combine(sessaoComTitulo, tique.onStart { emit(Unit) }) { par, _ -> par }
        .onEach { par -> par?.first?.let(::sincronizarSeExpirou) }
        .map { par -> par?.let { (s, titulo) -> estado(s, titulo) } ?: BatalhaUiState(encerrada = true) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, BatalhaUiState())

    private fun estado(s: Sessao, titulo: String): BatalhaUiState {
        val restante = SessaoMaquina.restanteMs(s, relogio.agora())
        val duracao = when (s.fase) {
            Fase.FOCO, Fase.PAUSADO -> s.focoMs
            Fase.DESCANSO -> s.descansoMs
            Fase.AGUARDANDO -> 0L
        }
        val progresso = if (duracao <= 0L) 0f else (1f - restante.toFloat() / duracao).coerceIn(0f, 1f)
        return BatalhaUiState(s.fase, titulo, s.rodadaAtual, s.rodadasTotais, restante, progresso)
    }

    /** Fallback do alarme: ao chegar a zero, a própria UI sincroniza (uma vez por fim de fase). */
    private fun sincronizarSeExpirou(s: Sessao) {
        val fim = s.faseTerminaEm ?: return
        if (s.fase != Fase.FOCO && s.fase != Fase.DESCANSO) return
        if (relogio.agora() < fim || ultimoFimSincronizado == fim) return
        ultimoFimSincronizado = fim
        viewModelScope.launch { sessaoRepo.sincronizar() }
    }

    fun pausar() = acao { sessaoRepo.pausar() }
    fun retomar() = acao { sessaoRepo.retomar() }
    fun pularDescanso() = acao { sessaoRepo.pularDescanso() }
    fun iniciarProxima() = acao { sessaoRepo.iniciarProxima() }
    fun desistir() = acao { sessaoRepo.desistir() }

    private fun acao(bloco: suspend () -> Unit) {
        viewModelScope.launch { bloco() }
    }
}
