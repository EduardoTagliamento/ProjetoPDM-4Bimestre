package com.exemplo.questpomodoro.ui.batalha

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.exemplo.questpomodoro.R
import com.exemplo.questpomodoro.domain.model.Fase
import com.exemplo.questpomodoro.ui.theme.Cores

@Composable
fun ConfirmarDesistencia(fase: Fase, rodadaAtual: Int, total: Int, aoConfirmar: () -> Unit, aoCancelar: () -> Unit) {
    val emBatalha = fase == Fase.FOCO || fase == Fase.PAUSADO
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = {
            Text(
                stringResource(if (emBatalha) R.string.batalha_desistir else R.string.batalha_sair_quest),
                style = MaterialTheme.typography.titleMedium,
                color = Cores.Vermelho,
            )
        },
        text = { Text(textoConfirmacaoDesistir(fase, rodadaAtual, total)) },
        confirmButton = {
            TextButton(onClick = aoConfirmar) {
                Text(stringResource(if (emBatalha) R.string.batalha_desistir else R.string.batalha_sair_quest), color = Cores.Vermelho)
            }
        },
        dismissButton = { TextButton(onClick = aoCancelar) { Text(stringResource(R.string.cancelar)) } },
        containerColor = Cores.Painel,
    )
}
