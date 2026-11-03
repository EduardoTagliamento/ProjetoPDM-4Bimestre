package com.exemplo.questpomodoro.ui.config

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.exemplo.questpomodoro.R
import com.exemplo.questpomodoro.container
import com.exemplo.questpomodoro.data.prefs.Configuracoes
import com.exemplo.questpomodoro.notification.LembreteAgendador
import com.exemplo.questpomodoro.notification.Notificador
import com.exemplo.questpomodoro.service.BloqueioStatus
import com.exemplo.questpomodoro.ui.bloqueio.rememberBloqueioAtivo
import com.exemplo.questpomodoro.ui.components.PixelButton
import com.exemplo.questpomodoro.ui.components.PixelPanel
import com.exemplo.questpomodoro.ui.theme.Cores

@Composable
fun configuracoesViewModel(): ConfiguracoesViewModel {
    val ctx = LocalContext.current
    return viewModel(
        factory = viewModelFactory {
            initializer {
                val app = ctx.applicationContext
                ConfiguracoesViewModel(app.container.config) { LembreteAgendador.reagendar(app, it) }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesScreen(aoVoltar: () -> Unit, aoAbrirApps: () -> Unit) {
    val ctx = LocalContext.current
    val vm = configuracoesViewModel()
    val c by vm.uiState.collectAsStateWithLifecycle()
    val bloqueioAtivo = rememberBloqueioAtivo()
    var notificacoes by remember { mutableStateOf(Notificador.podeNotificar(ctx)) }
    LifecycleResumeEffect(Unit) {
        notificacoes = Notificador.podeNotificar(ctx)
        onPauseOrDispose { }
    }
    var escolherHora by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = Cores.Fundo,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.config_titulo), style = MaterialTheme.typography.titleLarge, color = Cores.Ouro) },
                navigationIcon = {
                    IconButton(onClick = aoVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.voltar), tint = Cores.Borda)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cores.Fundo),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PixelPanel(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.config_duracoes), style = MaterialTheme.typography.titleMedium, color = Cores.Ouro)
                Stepper(stringResource(R.string.config_foco), c.focoMin, Configuracoes.FOCO_FAIXA, vm::mudarFoco)
                Stepper(stringResource(R.string.config_descanso), c.descansoMin, Configuracoes.DESCANSO_FAIXA, vm::mudarDescanso)
                Text(stringResource(R.string.config_proxima_batalha), style = MaterialTheme.typography.bodySmall, color = Cores.TextoSecundario)
            }

            PixelPanel(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = aoAbrirApps)) {
                Text(stringResource(R.string.config_apps_liberados, c.appsLiberados.size), style = MaterialTheme.typography.titleMedium, color = Cores.Ouro)
                Text(stringResource(R.string.config_apps_dica), style = MaterialTheme.typography.bodySmall, color = Cores.TextoSecundario)
            }

            PixelPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.config_lembrete), style = MaterialTheme.typography.titleMedium, color = Cores.Ouro, modifier = Modifier.weight(1f))
                    Switch(
                        checked = c.lembreteAtivo,
                        onCheckedChange = { vm.mudarLembrete(it, c.lembreteHora, c.lembreteMinuto) },
                        colors = SwitchDefaults.colors(checkedTrackColor = Cores.Verde),
                    )
                }
                if (c.lembreteAtivo) {
                    TextButton(onClick = { escolherHora = true }) {
                        Text(stringResource(R.string.config_lembrete_hora, "%02d:%02d".format(c.lembreteHora, c.lembreteMinuto)), color = Cores.Borda)
                    }
                }
            }

            PixelPanel(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.config_permissoes), style = MaterialTheme.typography.titleMedium, color = Cores.Ouro)
                StatusPermissao(stringResource(R.string.config_perm_acessibilidade), bloqueioAtivo) { BloqueioStatus.abrirConfiguracoes(ctx) }
                StatusPermissao(stringResource(R.string.config_perm_notificacoes), notificacoes) { abrirNotificacoes(ctx) }
            }
        }
    }

    if (escolherHora) {
        val estado = rememberTimePickerState(c.lembreteHora, c.lembreteMinuto, is24Hour = true)
        AlertDialog(
            onDismissRequest = { escolherHora = false },
            text = { TimePicker(estado) },
            confirmButton = {
                TextButton(onClick = { escolherHora = false; vm.mudarLembrete(true, estado.hour, estado.minute) }) {
                    Text(stringResource(R.string.ok), color = Cores.Ouro)
                }
            },
            dismissButton = { TextButton(onClick = { escolherHora = false }) { Text(stringResource(R.string.cancelar)) } },
            containerColor = Cores.Painel,
        )
    }
}

@Composable
private fun Stepper(rotulo: String, valor: Int, faixa: IntRange, aoMudar: (Int) -> Unit) {
    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        PixelButton("−", onClick = { aoMudar(valor - 1) }, enabled = valor > faixa.first)
        Text(
            "$valor min",
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(88.dp),
        )
        PixelButton("+", onClick = { aoMudar(valor + 1) }, enabled = valor < faixa.last)
    }
}

@Composable
private fun StatusPermissao(nome: String, ok: Boolean, aoAbrir: () -> Unit) {
    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(nome, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(if (ok) R.string.config_perm_ok else R.string.config_perm_off),
                style = MaterialTheme.typography.bodySmall,
                color = if (ok) Cores.Verde else Cores.Laranja,
            )
        }
        TextButton(onClick = aoAbrir) { Text(stringResource(R.string.config_abrir), color = Cores.Ouro) }
    }
}

private fun abrirNotificacoes(ctx: Context) {
    ctx.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
