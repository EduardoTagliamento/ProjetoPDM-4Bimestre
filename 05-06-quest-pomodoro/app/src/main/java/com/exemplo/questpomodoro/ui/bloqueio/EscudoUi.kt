package com.exemplo.questpomodoro.ui.bloqueio

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.exemplo.questpomodoro.R
import com.exemplo.questpomodoro.service.BloqueioStatus
import com.exemplo.questpomodoro.ui.components.PixelPanel
import com.exemplo.questpomodoro.ui.theme.Cores

/** Estado do bloqueio, reavaliado sempre que a tela volta ao primeiro plano. */
@Composable
fun rememberBloqueioAtivo(): Boolean {
    val ctx = LocalContext.current
    var ativo by remember { mutableStateOf(BloqueioStatus.ativo(ctx)) }
    LifecycleResumeEffect(Unit) {
        ativo = BloqueioStatus.ativo(ctx)
        onPauseOrDispose { }
    }
    return ativo
}

/** Aviso na tela de batalha quando o bloqueio está desativado. */
@Composable
fun AvisoBloqueio() {
    if (rememberBloqueioAtivo()) return
    PixelPanel(Modifier.fillMaxWidth(), borda = Cores.Laranja) {
        Text(stringResource(R.string.bloqueio_aviso_inativo), style = MaterialTheme.typography.bodyMedium, color = Cores.Laranja)
    }
}

/** Diálogo antes da batalha quando o Serviço de Acessibilidade não está ativo. */
@Composable
fun DialogoEscudo(aoAtivar: () -> Unit, aoBatalharSemBloqueio: () -> Unit, aoCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(stringResource(R.string.escudo_titulo), style = MaterialTheme.typography.titleMedium, color = Cores.Ouro) },
        text = { Text(stringResource(R.string.escudo_texto)) },
        confirmButton = { TextButton(onClick = aoAtivar) { Text(stringResource(R.string.escudo_ativar), color = Cores.Ouro) } },
        dismissButton = { TextButton(onClick = aoBatalharSemBloqueio) { Text(stringResource(R.string.escudo_sem_bloqueio)) } },
        containerColor = Cores.Painel,
    )
}
