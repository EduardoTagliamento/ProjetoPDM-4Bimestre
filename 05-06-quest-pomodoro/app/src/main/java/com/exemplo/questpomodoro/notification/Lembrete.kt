package com.exemplo.questpomodoro.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.exemplo.questpomodoro.container
import com.exemplo.questpomodoro.data.prefs.Configuracoes
import com.exemplo.questpomodoro.domain.atrasoAteProximo
import com.exemplo.questpomodoro.domain.textoLembrete
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Lembrete diário via AlarmManager (o WorkManager atrasava minutos no teste em emulador).
 * Um alarme por vez: ao disparar, posta (se houver quests pendentes) e agenda o próximo dia.
 */
object LembreteAgendador {
    fun reagendar(context: Context, c: Configuracoes) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendente(context)
        if (!c.lembreteAtivo) {
            am.cancel(pi)
            return
        }
        val quando = System.currentTimeMillis() + atrasoAteProximo(LocalDateTime.now(), c.lembreteHora, c.lembreteMinuto).toMillis()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, pi)
        }
    }

    private fun pendente(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        1,
        Intent(context, LembreteReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

class LembreteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendente = goAsync()
        val c = context.container
        c.escopo.launch {
            try {
                textoLembrete(c.questRepository.contarPendentes())?.let {
                    Notificador.postar(context, IdsNotificacao.LEMBRETE, Notificador.lembrete(context, it))
                }
                LembreteAgendador.reagendar(context, c.config.atual())
            } finally {
                pendente.finish()
            }
        }
    }
}
