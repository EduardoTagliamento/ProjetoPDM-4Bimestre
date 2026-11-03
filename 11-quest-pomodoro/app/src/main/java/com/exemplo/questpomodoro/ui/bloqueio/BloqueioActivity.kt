package com.exemplo.questpomodoro.ui.bloqueio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.exemplo.questpomodoro.MainActivity
import com.exemplo.questpomodoro.R
import com.exemplo.questpomodoro.container
import com.exemplo.questpomodoro.domain.SessaoMaquina
import com.exemplo.questpomodoro.domain.formatarTempo
import com.exemplo.questpomodoro.domain.model.Fase
import com.exemplo.questpomodoro.notification.Notificador
import com.exemplo.questpomodoro.ui.batalha.ConfirmarDesistencia
import com.exemplo.questpomodoro.ui.components.PixelButton
import com.exemplo.questpomodoro.ui.components.SpritePixel
import com.exemplo.questpomodoro.ui.components.Sprites
import com.exemplo.questpomodoro.ui.theme.Cores
import com.exemplo.questpomodoro.ui.theme.TemaRpg
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** Tela cheia mostrada quando o usuário abre um app bloqueado durante o foco. */
class BloqueioActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this) { voltarABatalha() }
        setContent {
            TemaRpg {
                BloqueioScreen(
                    aoVoltar = ::voltarABatalha,
                    aoDesistir = {
                        lifecycleScope.launch {
                            container.sessaoRepository.desistir()
                            voltarABatalha()
                        }
                    },
                    aoForaDoFoco = ::finish,
                )
            }
        }
    }

    private fun voltarABatalha() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(Notificador.EXTRA_DESTINO, Notificador.DESTINO_BATALHA)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }
}

@Composable
private fun BloqueioScreen(aoVoltar: () -> Unit, aoDesistir: () -> Unit, aoForaDoFoco: () -> Unit) {
    val c = androidx.compose.ui.platform.LocalContext.current.container
    val sessao by c.sessaoRepository.sessao.collectAsState(initial = null)
    val quest by remember(sessao?.questId) {
        sessao?.questId?.let { c.questRepository.observarQuest(it) } ?: flowOf(null)
    }.collectAsState(initial = null)
    var agora by remember { mutableLongStateOf(c.relogio.agora()) }
    var confirmar by rememberSaveable { mutableStateOf(false) }
    var carregou by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            agora = c.relogio.agora()
            delay(1000)
        }
    }
    LaunchedEffect(sessao) {
        if (sessao != null) carregou = true
        if (carregou && sessao?.fase != Fase.FOCO) aoForaDoFoco()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Cores.Fundo)
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SpritePixel(Sprites.Escudo, Modifier.size(120.dp))
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.bloqueio_titulo),
            style = MaterialTheme.typography.headlineSmall,
            color = Cores.Vermelho,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(quest?.titulo ?: "", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        sessao?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.bloqueio_restante, formatarTempo(SessaoMaquina.restanteMs(it, agora))),
                style = MaterialTheme.typography.titleMedium,
                color = Cores.Ouro,
            )
        }
        Spacer(Modifier.height(32.dp))
        PixelButton(stringResource(R.string.bloqueio_voltar), onClick = aoVoltar, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        PixelButton(stringResource(R.string.batalha_desistir), onClick = { confirmar = true }, cor = Cores.Aco, modifier = Modifier.fillMaxWidth())
    }

    val s = sessao
    if (confirmar && s != null) {
        ConfirmarDesistencia(
            fase = s.fase,
            rodadaAtual = s.rodadaAtual,
            total = s.rodadasTotais,
            aoConfirmar = { confirmar = false; aoDesistir() },
            aoCancelar = { confirmar = false },
        )
    }
}
