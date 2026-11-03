package com.exemplo.questpomodoro.ui.quest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.exemplo.questpomodoro.R
import com.exemplo.questpomodoro.container
import com.exemplo.questpomodoro.domain.ErroQuest
import com.exemplo.questpomodoro.domain.QuestRegras
import com.exemplo.questpomodoro.domain.model.Prioridade
import com.exemplo.questpomodoro.ui.components.PixelButton
import com.exemplo.questpomodoro.ui.components.PixelPanel
import com.exemplo.questpomodoro.ui.taverna.corDaRaridade
import com.exemplo.questpomodoro.ui.theme.Cores

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestFormScreen(questId: Long, aoVoltar: () -> Unit) {
    val ctx = LocalContext.current
    val vm: QuestFormViewModel = viewModel(
        key = "quest-$questId",
        factory = viewModelFactory {
            initializer { ctx.container.let { QuestFormViewModel(questId, it.questRepository, it.sessaoRepository, it.config) } }
        },
    )
    val s by vm.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmarExclusao by rememberSaveable { mutableStateOf(false) }
    val msgBatalha = stringResource(R.string.form_recusada_batalha)

    LaunchedEffect(s.concluido) { if (s.concluido) aoVoltar() }
    LaunchedEffect(s.recusadaEmBatalha) {
        if (s.recusadaEmBatalha) {
            snackbar.showSnackbar(msgBatalha)
            vm.avisoVisto()
        }
    }

    Scaffold(
        containerColor = Cores.Fundo,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (s.editando) R.string.form_titulo_editar else R.string.form_titulo_nova),
                        style = MaterialTheme.typography.titleLarge,
                        color = Cores.Ouro,
                    )
                },
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
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (s.somenteLeitura) {
                PixelPanel(Modifier.fillMaxWidth(), borda = Cores.Laranja) {
                    Text(
                        stringResource(if (s.questConcluida) R.string.form_aviso_concluida else R.string.form_aviso_batalha),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            val cores = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Cores.Ouro,
                unfocusedBorderColor = Cores.Aco,
                focusedLabelColor = Cores.Ouro,
                cursorColor = Cores.Ouro,
            )
            OutlinedTextField(
                value = s.titulo,
                onValueChange = vm::mudarTitulo,
                label = { Text(stringResource(R.string.form_campo_titulo)) },
                singleLine = true,
                enabled = !s.somenteLeitura,
                isError = ErroQuest.TITULO_VAZIO in s.erros || ErroQuest.TITULO_LONGO in s.erros,
                supportingText = {
                    Row(Modifier.fillMaxWidth()) {
                        Text(mensagemTitulo(s.erros) ?: "", Modifier.weight(1f))
                        Text("${s.titulo.trim().length}/${QuestRegras.TITULO_MAX}")
                    }
                },
                colors = cores,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = s.descricao,
                onValueChange = vm::mudarDescricao,
                label = { Text(stringResource(R.string.form_campo_descricao)) },
                enabled = !s.somenteLeitura,
                minLines = 2,
                colors = cores,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.form_raridade), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Prioridade.entries.forEach { p ->
                    val sel = p == s.prioridade
                    Box(
                        Modifier
                            .weight(1f)
                            .background(if (sel) corDaRaridade(p) else Cores.Fundo)
                            .border(2.dp, corDaRaridade(p))
                            .clickable(enabled = !s.somenteLeitura, role = Role.RadioButton) { vm.mudarPrioridade(p) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(p.raridade, style = MaterialTheme.typography.labelMedium, color = if (sel) Cores.Fundo else corDaRaridade(p))
                    }
                }
            }

            Text(stringResource(R.string.form_batalhas), style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelButton("−", onClick = { vm.mudarRodadas(s.rodadas - 1) }, enabled = !s.somenteLeitura)
                Text(
                    "${s.rodadas}",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.width(80.dp).padding(horizontal = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                PixelButton("+", onClick = { vm.mudarRodadas(s.rodadas + 1) }, enabled = !s.somenteLeitura)
            }
            mensagemRodadas(s.erros, s.rodadasConcluidas)?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            PixelPanel(Modifier.fillMaxWidth(), borda = Cores.Azul) {
                Text(s.previa, style = MaterialTheme.typography.bodyLarge)
            }

            if (!s.somenteLeitura) {
                PixelButton(stringResource(R.string.form_salvar), onClick = vm::salvar, modifier = Modifier.fillMaxWidth())
            }
            if (s.editando) {
                PixelButton(
                    stringResource(R.string.form_excluir),
                    onClick = { confirmarExclusao = true },
                    cor = Cores.Vermelho,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            text = { Text(stringResource(R.string.form_confirmar_excluir)) },
            confirmButton = {
                TextButton(onClick = { confirmarExclusao = false; vm.excluir() }) { Text(stringResource(R.string.form_excluir), color = Cores.Vermelho) }
            },
            dismissButton = { TextButton(onClick = { confirmarExclusao = false }) { Text(stringResource(R.string.cancelar)) } },
            containerColor = Cores.Painel,
        )
    }
}

@Composable
private fun mensagemTitulo(erros: Set<ErroQuest>): String? = when {
    ErroQuest.TITULO_VAZIO in erros -> stringResource(R.string.erro_titulo_vazio)
    ErroQuest.TITULO_LONGO in erros -> stringResource(R.string.erro_titulo_longo)
    else -> null
}

@Composable
private fun mensagemRodadas(erros: Set<ErroQuest>, concluidas: Int): String? = when {
    ErroQuest.RODADAS_FORA_DO_INTERVALO in erros -> stringResource(R.string.erro_rodadas_intervalo)
    ErroQuest.RODADAS_ABAIXO_DO_CONCLUIDO in erros -> stringResource(R.string.erro_rodadas_abaixo, concluidas)
    else -> null
}
