package com.exemplo.questpomodoro.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exemplo.questpomodoro.data.prefs.Configuracoes
import com.exemplo.questpomodoro.data.prefs.ConfiguracoesStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppInstalado(val pacote: String, val nome: String)

class ConfiguracoesViewModel(
    private val config: ConfiguracoesStore,
    private val aoMudarLembrete: (Configuracoes) -> Unit,
) : ViewModel() {

    val uiState: StateFlow<Configuracoes> = config.configuracoes.stateIn(viewModelScope, SharingStarted.Eagerly, Configuracoes())

    fun mudarFoco(min: Int) = atualizar { it.copy(focoMin = min) }

    fun mudarDescanso(min: Int) = atualizar { it.copy(descansoMin = min) }

    fun alternarApp(pacote: String) = atualizar {
        it.copy(appsLiberados = if (pacote in it.appsLiberados) it.appsLiberados - pacote else it.appsLiberados + pacote)
    }

    fun mudarLembrete(ativo: Boolean, hora: Int, minuto: Int) {
        viewModelScope.launch {
            config.atualizar { it.copy(lembreteAtivo = ativo, lembreteHora = hora, lembreteMinuto = minuto) }
            aoMudarLembrete(config.atual())
        }
    }

    private fun atualizar(t: (Configuracoes) -> Configuracoes) {
        viewModelScope.launch { config.atualizar(t) }
    }
}
