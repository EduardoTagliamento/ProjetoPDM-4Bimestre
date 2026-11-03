package com.exemplo.questpomodoro.ui.taverna

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exemplo.questpomodoro.data.prefs.HeroiStore
import com.exemplo.questpomodoro.data.repository.QuestRepository
import com.exemplo.questpomodoro.data.repository.SessaoRepository
import com.exemplo.questpomodoro.domain.Filtro
import com.exemplo.questpomodoro.domain.Heroi
import com.exemplo.questpomodoro.domain.Ordem
import com.exemplo.questpomodoro.domain.QuestRegras
import com.exemplo.questpomodoro.domain.model.Quest
import com.exemplo.questpomodoro.domain.model.Vitoria
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TavernaUiState(
    val nivel: Int = 1,
    val xpNoNivel: Int = 0,
    val quests: List<Quest> = emptyList(),
    val filtro: Filtro = Filtro.TODAS,
    val ordem: Ordem = Ordem.PRIORIDADE,
    val sessaoQuestId: Long? = null,
    val sessaoTitulo: String? = null,
    val vitoria: Vitoria? = null,
)

class TavernaViewModel(
    questRepo: QuestRepository,
    private val sessaoRepo: SessaoRepository,
    private val heroi: HeroiStore,
) : ViewModel() {

    private val visao = MutableStateFlow(Filtro.TODAS to Ordem.PRIORIDADE)

    val uiState: StateFlow<TavernaUiState> = combine(
        questRepo.observarQuests(),
        visao,
        sessaoRepo.sessao,
        heroi.xp,
        heroi.vitoriaPendente,
    ) { quests, (filtro, ordem), sessao, xp, vitoria ->
        TavernaUiState(
            nivel = Heroi.nivel(xp),
            xpNoNivel = Heroi.xpNoNivel(xp),
            quests = QuestRegras.filtrarEOrdenar(quests, filtro, ordem),
            filtro = filtro,
            ordem = ordem,
            sessaoQuestId = sessao?.questId,
            sessaoTitulo = sessao?.let { s -> quests.firstOrNull { it.id == s.questId }?.titulo },
            vitoria = vitoria,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TavernaUiState())

    fun mudarFiltro(f: Filtro) = visao.update { it.copy(first = f) }

    fun mudarOrdem(o: Ordem) = visao.update { it.copy(second = o) }

    fun iniciarBatalha(questId: Long, aoIniciar: () -> Unit) {
        viewModelScope.launch {
            if (sessaoRepo.iniciar(questId)) aoIniciar()
        }
    }

    fun dispensarVitoria() {
        viewModelScope.launch { heroi.limparVitoria() }
    }
}
