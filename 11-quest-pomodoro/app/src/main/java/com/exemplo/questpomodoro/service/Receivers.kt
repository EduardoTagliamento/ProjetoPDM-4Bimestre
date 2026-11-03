package com.exemplo.questpomodoro.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.exemplo.questpomodoro.container
import com.exemplo.questpomodoro.notification.AcaoNotificacao
import com.exemplo.questpomodoro.notification.LembreteAgendador
import kotlinx.coroutines.launch

/** Roda [bloco] fora da main thread mantendo o receiver vivo até terminar. */
private fun BroadcastReceiver.assincrono(context: Context, bloco: suspend () -> Unit) {
    val pendente = goAsync()
    context.container.escopo.launch {
        try {
            bloco()
        } finally {
            pendente.finish()
        }
    }
}

/** Disparado no fim de cada fase. */
class FaseAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = assincrono(context) {
        context.container.sessaoRepository.sincronizar()
    }
}

/** Reagenda o alarme e o serviço depois de reiniciar o aparelho ou atualizar o app. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        assincrono(context) {
            val c = context.container
            c.restaurarSessao()
            LembreteAgendador.reagendar(context, c.config.atual())
        }
    }
}

/** Ações das notificações: Retomar / Iniciar próxima batalha. */
class SessaoAcaoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val acao = intent.getStringExtra(EXTRA_ACAO)?.let { runCatching { AcaoNotificacao.valueOf(it) }.getOrNull() } ?: return
        assincrono(context) {
            val repo = context.container.sessaoRepository
            when (acao) {
                AcaoNotificacao.RETOMAR -> repo.retomar()
                AcaoNotificacao.INICIAR_PROXIMA -> repo.iniciarProxima()
            }
        }
    }

    companion object {
        const val ACTION = "com.exemplo.questpomodoro.ACAO"
        const val EXTRA_ACAO = "acao"
    }
}
