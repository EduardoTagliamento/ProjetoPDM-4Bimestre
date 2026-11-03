package com.exemplo.questpomodoro.ui.quest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exemplo.questpomodoro.data.prefs.ConfiguracoesStore
import com.exemplo.questpomodoro.data.repository.QuestRepository
import com.exemplo.questpomodoro.data.repository.ResultadoSalvar
import com.exemplo.questpomodoro.data.repository.SessaoRepository
import com.exemplo.questpomodoro.domain.ErroQuest
import com.exemplo.questpomodoro.domain.QuestRegras
import com.exemplo.questpomodoro.domain.model.Prioridade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuestFormUiState(
    val titulo: String = "",
    val descricao: String = "",
    val prioridade: Prioridade = Prioridade.MEDIA,
    val rodadas: Int = 4,
    val rodadasConcluidas: Int = 0,
    val previa: String = "",
    val erros: Set<ErroQuest> = emptySet(),
    val editando: Boolean = false,
    val somenteLeitura: Boolean = false,
    val questConcluida: Boolean = false,
    /** true quando uma ação foi recusada porque a quest está em batalha. */
    val recusadaEmBatalha: Boolean = false,
    /** Salvou ou excluiu: a tela deve voltar. */
    val concluido: Boolean = false,
)

class QuestFormViewModel(
    private val questId: Long,
    private val questRepo: QuestRepository,
    sessaoRepo: SessaoRepository,
    config: ConfiguracoesStore,
) : ViewModel() {

    private val form = MutableStateFlow(QuestFormUiState(editando = questId != 0L))

    val uiState: StateFlow<QuestFormUiState> = combine(form, config.configuracoes, sessaoRepo.sessao) { f, c, sessao ->
        val emBatalha = questId != 0L && sessao?.questId == questId
        f.copy(
            previa = QuestRegras.textoPrevia(f.rodadas, c.focoMin, c.descansoMin),
            somenteLeitura = f.questConcluida || emBatalha,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, form.value)

    init {
        if (questId != 0L) {
            viewModelScope.launch {
                val q = questRepo.observarQuest(questId).first() ?: return@launch
                form.update {
                    it.copy(
                        titulo = q.titulo,
                        descricao = q.descricao,
                        prioridade = Prioridade.de(q.prioridade),
                        rodadas = q.rodadasTotais,
                        rodadasConcluidas = q.rodadasConcluidas,
                        questConcluida = q.concluida,
                    )
                }
            }
        }
    }

    fun mudarTitulo(t: String) = form.update { it.copy(titulo = t, erros = it.erros - ErroQuest.TITULO_VAZIO - ErroQuest.TITULO_LONGO) }

    fun mudarDescricao(d: String) = form.update { it.copy(descricao = d) }

    fun mudarPrioridade(p: Prioridade) = form.update { it.copy(prioridade = p) }

    fun mudarRodadas(n: Int) = form.update {
        it.copy(rodadas = n.coerceIn(maxOf(QuestRegras.RODADAS.first, it.rodadasConcluidas), QuestRegras.RODADAS.last))
    }

    fun salvar() {
        val f = form.value
        viewModelScope.launch {
            when (val r = questRepo.salvar(questId, f.titulo, f.descricao, f.prioridade.valor, f.rodadas)) {
                is ResultadoSalvar.Sucesso -> form.update { it.copy(concluido = true, erros = emptySet()) }
                is ResultadoSalvar.Invalido -> form.update { it.copy(erros = r.erros) }
                ResultadoSalvar.Bloqueada -> form.update { it.copy(recusadaEmBatalha = true) }
            }
        }
    }

    fun excluir() {
        viewModelScope.launch {
            if (questRepo.excluir(questId)) form.update { it.copy(concluido = true) }
            else form.update { it.copy(recusadaEmBatalha = true) }
        }
    }

    fun avisoVisto() = form.update { it.copy(recusadaEmBatalha = false) }
}
