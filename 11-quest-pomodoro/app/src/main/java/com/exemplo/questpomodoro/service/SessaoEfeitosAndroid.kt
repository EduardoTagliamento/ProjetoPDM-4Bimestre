package com.exemplo.questpomodoro.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.exemplo.questpomodoro.data.repository.Alerta
import com.exemplo.questpomodoro.data.repository.SessaoEfeitos
import com.exemplo.questpomodoro.domain.model.Fase
import com.exemplo.questpomodoro.domain.model.Sessao
import com.exemplo.questpomodoro.notification.IdsNotificacao
import com.exemplo.questpomodoro.notification.Notificador
import com.exemplo.questpomodoro.notification.conteudoAlerta

class SessaoEfeitosAndroid(private val context: Context) : SessaoEfeitos {
    private val alarme = AgendadorAlarme(context)

    override fun aoMudarSessao(sessao: Sessao?) {
        val fim = sessao?.faseTerminaEm
        if (fim != null) alarme.agendar(fim) else alarme.cancelar()
        if (sessao != null) iniciarServico()
        // "De volta à batalha!" perde o sentido quando a batalha já começou.
        if (sessao?.fase == Fase.FOCO) NotificationManagerCompat.from(context).cancel(IdsNotificacao.ALERTA)
    }

    override fun alertar(alerta: Alerta) {
        Notificador.postar(context, IdsNotificacao.ALERTA, Notificador.alerta(context, conteudoAlerta(alerta)))
    }

    private fun iniciarServico() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, SessaoService::class.java))
        } catch (e: IllegalStateException) {
            // Início em segundo plano não permitido; o alarme continua valendo e a UI reinicia o serviço.
            Log.w("QuestPomodoro", "Não foi possível iniciar o SessaoService", e)
        }
    }
}
