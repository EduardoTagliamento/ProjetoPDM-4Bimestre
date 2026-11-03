package com.exemplo.questpomodoro.ui.batalha

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.exemplo.questpomodoro.R
import com.exemplo.questpomodoro.container
import com.exemplo.questpomodoro.domain.formatarTempo
import com.exemplo.questpomodoro.domain.model.Fase
import com.exemplo.questpomodoro.ui.components.PixelButton
import com.exemplo.questpomodoro.ui.components.PixelPanel
import com.exemplo.questpomodoro.ui.components.SpritePixel
import com.exemplo.questpomodoro.ui.components.Sprites
import com.exemplo.questpomodoro.ui.components.TimerCircular
import com.exemplo.questpomodoro.ui.theme.Cores

@Composable
fun BatalhaScreen(aoVoltarTaverna: () -> Unit, avisoBloqueio: @Composable () -> Unit = {}) {
    val ctx = LocalContext.current
    val vm: BatalhaViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ctx.container.let { BatalhaViewModel(it.sessaoRepository, it.questRepository, it.relogio) } }
        },
    )
    val s by vm.uiState.collectAsStateWithLifecycle()
    var confirmar by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(s.encerrada) { if (s.encerrada) aoVoltarTaverna() }
    BackHandler(onBack = aoVoltarTaverna)

    val fase = s.fase ?: return
    val emBatalha = fase == Fase.FOCO || fase == Fase.PAUSADO

    Column(
        Modifier
            .fillMaxSize()
            .background(Cores.Fundo)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(s.titulo, style = MaterialTheme.typography.titleLarge, color = Cores.Ouro, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)

        when (fase) {
            Fase.FOCO, Fase.PAUSADO -> {
                SpritePixel(Sprites.Espada, Modifier.size(72.dp))
                Text(stringResource(R.string.batalha_n, s.rodadaAtual, s.rodadasTotais), style = MaterialTheme.typography.titleMedium)
                if (fase == Fase.FOCO) avisoBloqueio()
                TimerCircular(s.progresso, formatarTempo(s.restanteMs), Cores.Vermelho, Modifier.widthIn(max = 280.dp).fillMaxWidth())
                if (fase == Fase.PAUSADO) {
                    PixelPanel(borda = Cores.Laranja) {
                        Text(stringResource(R.string.batalha_pausado), style = MaterialTheme.typography.labelMedium, color = Cores.Laranja)
                    }
                    PixelButton(stringResource(R.string.batalha_retomar), onClick = vm::retomar, modifier = Modifier.fillMaxWidth())
                } else {
                    PixelButton(stringResource(R.string.batalha_pausar), onClick = vm::pausar, cor = Cores.Azul, modifier = Modifier.fillMaxWidth())
                }
            }
            Fase.DESCANSO -> {
                Row(verticalAlignment = Alignment.Bottom) {
                    SpritePixel(Sprites.Fogueira, Modifier.size(72.dp))
                    SpritePixel(Sprites.Caneca, Modifier.size(48.dp))
                }
                Text(stringResource(R.string.pousada_titulo), style = MaterialTheme.typography.titleMedium, color = Cores.Azul)
                TimerCircular(s.progresso, formatarTempo(s.restanteMs), Cores.Azul, Modifier.widthIn(max = 280.dp).fillMaxWidth())
                Text(stringResource(R.string.pousada_proxima, s.rodadaAtual, s.rodadasTotais), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.pousada_apps_liberados), style = MaterialTheme.typography.bodySmall, color = Cores.TextoSecundario)
                PixelButton(stringResource(R.string.pousada_pular), onClick = vm::pularDescanso, cor = Cores.Azul, modifier = Modifier.fillMaxWidth())
            }
            Fase.AGUARDANDO -> {
                Row(verticalAlignment = Alignment.Bottom) {
                    SpritePixel(Sprites.Fogueira, Modifier.size(72.dp))
                    SpritePixel(Sprites.Espada, Modifier.size(56.dp))
                }
                Text(stringResource(R.string.aguardando_titulo), style = MaterialTheme.typography.titleMedium, color = Cores.Ouro, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                PixelButton(
                    stringResource(R.string.aguardando_iniciar, s.rodadaAtual, s.rodadasTotais),
                    onClick = vm::iniciarProxima,
                    cor = Cores.Vermelho,
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                )
            }
        }

        PixelButton(
            stringResource(if (emBatalha) R.string.batalha_desistir else R.string.batalha_sair_quest),
            onClick = { confirmar = true },
            cor = Cores.Aco,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (confirmar) {
        ConfirmarDesistencia(
            fase = fase,
            rodadaAtual = s.rodadaAtual,
            total = s.rodadasTotais,
            aoConfirmar = { confirmar = false; vm.desistir() },
            aoCancelar = { confirmar = false },
        )
    }
}
