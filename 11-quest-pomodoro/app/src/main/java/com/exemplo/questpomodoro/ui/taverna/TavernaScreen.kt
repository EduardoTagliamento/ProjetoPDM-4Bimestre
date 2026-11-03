package com.exemplo.questpomodoro.ui.taverna

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.exemplo.questpomodoro.R
import com.exemplo.questpomodoro.container
import com.exemplo.questpomodoro.domain.Filtro
import com.exemplo.questpomodoro.domain.Ordem
import com.exemplo.questpomodoro.domain.model.Prioridade
import com.exemplo.questpomodoro.domain.model.Quest
import com.exemplo.questpomodoro.ui.components.BarraXp
import com.exemplo.questpomodoro.ui.components.PixelButton
import com.exemplo.questpomodoro.ui.components.PixelPanel
import com.exemplo.questpomodoro.ui.components.ProgressoRodadas
import com.exemplo.questpomodoro.ui.components.SpritePixel
import com.exemplo.questpomodoro.ui.components.Sprites
import com.exemplo.questpomodoro.ui.theme.Cores

/**
 * [aoIniciarBatalha] decide se a batalha começa (ex.: diálogo do bloqueio) e chama `iniciar` quando for o caso.
 */
@Composable
fun TavernaScreen(
    aoAbrirQuest: (Long) -> Unit,
    aoIrParaBatalha: () -> Unit,
    aoAbrirConfig: () -> Unit,
    aoPedirBatalha: (questId: Long, iniciar: () -> Unit) -> Unit = { _, iniciar -> iniciar() },
) {
    val ctx = LocalContext.current
    val vm: TavernaViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ctx.container.let { TavernaViewModel(it.questRepository, it.sessaoRepository, it.heroi) } }
        },
    )
    val estado by vm.uiState.collectAsStateWithLifecycle()

    val pedirNotificacao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Scaffold(
        containerColor = Cores.Fundo,
        floatingActionButton = {
            PixelButton(stringResource(R.string.taverna_nova_quest), onClick = { aoAbrirQuest(0) })
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { CabecalhoHeroi(estado.nivel, estado.xpNoNivel, aoAbrirConfig) }
            if (estado.sessaoQuestId != null) {
                item {
                    PixelPanel(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = aoIrParaBatalha), borda = Cores.Vermelho) {
                        Text(stringResource(R.string.taverna_em_batalha), style = MaterialTheme.typography.labelMedium, color = Cores.Vermelho)
                        Spacer(Modifier.height(6.dp))
                        Text(estado.sessaoTitulo ?: "", style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            item { BarraFiltros(estado.filtro, estado.ordem, vm::mudarFiltro, vm::mudarOrdem) }
            if (estado.quests.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.taverna_vazia),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Cores.TextoSecundario,
                        modifier = Modifier.padding(vertical = 32.dp),
                    )
                }
            }
            items(estado.quests, key = { it.id }) { quest ->
                CartaoQuest(
                    quest = quest,
                    podeBatalhar = estado.sessaoQuestId == null && !quest.concluida,
                    aoAbrir = { aoAbrirQuest(quest.id) },
                    aoBatalhar = { aoPedirBatalha(quest.id) { vm.iniciarBatalha(quest.id, aoIrParaBatalha) } },
                )
            }
        }
    }

    estado.vitoria?.let { VitoriaDialog(it, vm::dispensarVitoria) }
}

@Composable
private fun CabecalhoHeroi(nivel: Int, xpNoNivel: Int, aoAbrirConfig: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SpritePixel(Sprites.Heroi, Modifier.size(64.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.taverna_titulo), style = MaterialTheme.typography.headlineSmall, color = Cores.Ouro)
            Spacer(Modifier.height(8.dp))
            BarraXp(nivel, xpNoNivel, Modifier.fillMaxWidth())
        }
        IconButton(onClick = aoAbrirConfig) {
            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.config_titulo), tint = Cores.Borda)
        }
    }
}

@Composable
private fun BarraFiltros(filtro: Filtro, ordem: Ordem, aoFiltrar: (Filtro) -> Unit, aoOrdenar: (Ordem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Filtro.TODAS to R.string.filtro_todas,
                Filtro.PENDENTES to R.string.filtro_pendentes,
                Filtro.CONCLUIDAS to R.string.filtro_concluidas,
            ).forEach { (f, rotulo) -> Etiqueta(stringResource(rotulo), selecionada = f == filtro) { aoFiltrar(f) } }
        }
        val proxima = if (ordem == Ordem.PRIORIDADE) Ordem.DATA else Ordem.PRIORIDADE
        val rotuloOrdem = if (ordem == Ordem.PRIORIDADE) R.string.ordem_prioridade else R.string.ordem_data
        Etiqueta(stringResource(R.string.ordem_rotulo, stringResource(rotuloOrdem)), selecionada = false) { aoOrdenar(proxima) }
    }
}

@Composable
private fun Etiqueta(texto: String, selecionada: Boolean, aoClicar: () -> Unit) {
    Box(
        Modifier
            .background(if (selecionada) Cores.Ouro else Cores.Fundo)
            .border(2.dp, if (selecionada) Cores.Ouro else Cores.Aco)
            .clickable(role = Role.Tab, onClick = aoClicar)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Text(texto, style = MaterialTheme.typography.labelMedium, color = if (selecionada) Cores.Fundo else Cores.Borda)
    }
}

fun corDaRaridade(p: Prioridade): Color = when (p) {
    Prioridade.BAIXA -> Cores.TextoSecundario
    Prioridade.MEDIA -> Cores.Azul
    Prioridade.ALTA -> Cores.Ouro
}

@Composable
private fun CartaoQuest(quest: Quest, podeBatalhar: Boolean, aoAbrir: () -> Unit, aoBatalhar: () -> Unit) {
    val prioridade = Prioridade.de(quest.prioridade)
    PixelPanel(Modifier.fillMaxWidth().clickable(onClick = aoAbrir), borda = if (quest.concluida) Cores.Verde else Cores.Borda) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(prioridade.raridade.uppercase(), style = MaterialTheme.typography.labelSmall, color = corDaRaridade(prioridade))
                Spacer(Modifier.height(6.dp))
                Text(quest.titulo, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                ProgressoRodadas(quest.rodadasConcluidas, quest.rodadasTotais)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (quest.concluida) stringResource(R.string.quest_concluida_rotulo)
                    else stringResource(R.string.quest_progresso, quest.rodadasConcluidas, quest.rodadasTotais),
                    style = MaterialTheme.typography.bodySmall,
                    color = Cores.TextoSecundario,
                )
            }
            if (!quest.concluida) {
                Spacer(Modifier.width(8.dp))
                PixelButton("⚔", onClick = aoBatalhar, enabled = podeBatalhar, cor = Cores.Vermelho)
            }
        }
    }
}
