package com.exemplo.questpomodoro.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.exemplo.questpomodoro.service.BloqueioStatus
import com.exemplo.questpomodoro.ui.batalha.BatalhaScreen
import com.exemplo.questpomodoro.ui.bloqueio.AvisoBloqueio
import com.exemplo.questpomodoro.ui.bloqueio.DialogoEscudo
import com.exemplo.questpomodoro.ui.quest.QuestFormScreen
import com.exemplo.questpomodoro.ui.taverna.TavernaScreen

object Rotas {
    const val TAVERNA = "taverna"
    const val QUEST = "quest/{id}"
    fun quest(id: Long) = "quest/$id"
    const val BATALHA = "batalha"
    const val CONFIG = "config"
    const val APPS = "config/apps"
}

/** [pedidoDestino] muda a cada intent recebida (ex.: toque na notificação). */
@Composable
fun Navegacao(destinoInicial: String?, pedidoDestino: Int = 0) {
    val nav = rememberNavController()

    LaunchedEffect(destinoInicial, pedidoDestino) {
        if (destinoInicial == Rotas.BATALHA) {
            nav.navigate(Rotas.BATALHA) { launchSingleTop = true }
        }
    }

    NavHost(nav, startDestination = Rotas.TAVERNA) {
        composable(Rotas.TAVERNA) {
            val ctx = LocalContext.current
            var pendente by remember { mutableStateOf<(() -> Unit)?>(null) }
            TavernaScreen(
                aoAbrirQuest = { nav.navigate(Rotas.quest(it)) },
                aoIrParaBatalha = { nav.navigate(Rotas.BATALHA) { launchSingleTop = true } },
                aoAbrirConfig = { nav.navigate(Rotas.CONFIG) },
                aoPedirBatalha = { _, iniciar -> if (BloqueioStatus.ativo(ctx)) iniciar() else pendente = iniciar },
            )
            pendente?.let { iniciar ->
                DialogoEscudo(
                    aoAtivar = { pendente = null; BloqueioStatus.abrirConfiguracoes(ctx) },
                    aoBatalharSemBloqueio = { pendente = null; iniciar() },
                    aoCancelar = { pendente = null },
                )
            }
        }
        composable(Rotas.QUEST, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entrada ->
            QuestFormScreen(
                questId = entrada.arguments?.getLong("id") ?: 0L,
                aoVoltar = { nav.popBackStack() },
            )
        }
        composable(Rotas.BATALHA) {
            BatalhaScreen(
                aoVoltarTaverna = { if (!nav.popBackStack(Rotas.TAVERNA, false)) nav.navigate(Rotas.TAVERNA) },
                avisoBloqueio = { AvisoBloqueio() },
            )
        }
        composable(Rotas.CONFIG) {
            Text("Configurações")
        }
    }
}
