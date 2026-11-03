package com.exemplo.questpomodoro.notification

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.exemplo.questpomodoro.MainActivity
import com.exemplo.questpomodoro.R
import com.exemplo.questpomodoro.service.SessaoAcaoReceiver

object Notificador {
    const val EXTRA_DESTINO = "destino"
    const val DESTINO_BATALHA = "batalha"

    fun continua(context: Context, c: ConteudoNotificacao): Notification =
        base(context, Canais.CONTINUO, c)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    fun preparando(context: Context): Notification =
        continua(context, ConteudoNotificacao(context.getString(R.string.app_name), context.getString(R.string.notif_preparando), null, null))

    fun alerta(context: Context, c: ConteudoNotificacao): Notification =
        base(context, Canais.ALERTAS, c)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

    fun lembrete(context: Context, texto: String): Notification =
        base(context, Canais.LEMBRETES, ConteudoNotificacao(context.getString(R.string.app_name), texto, null, null))
            .setAutoCancel(true)
            .build()

    /** Posta se houver permissão; sem permissão, não faz nada. */
    fun postar(context: Context, id: Int, n: Notification) {
        if (!podeNotificar(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
            // Permissão revogada entre a checagem e o envio.
        }
    }

    fun podeNotificar(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun base(context: Context, canal: String, c: ConteudoNotificacao): NotificationCompat.Builder {
        val b = NotificationCompat.Builder(context, canal)
            .setSmallIcon(R.drawable.ic_espada)
            .setContentTitle(c.titulo)
            .setContentText(c.texto)
            .setColor(ContextCompat.getColor(context, R.color.ouro))
            .setContentIntent(abrirBatalha(context))
        if (c.cronometroAte != null) {
            b.setUsesChronometer(true).setChronometerCountDown(true).setWhen(c.cronometroAte).setShowWhen(true)
        }
        c.acao?.let { b.addAction(acao(context, it)) }
        return b
    }

    private fun abrirBatalha(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_DESTINO, DESTINO_BATALHA)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, 10, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun acao(context: Context, a: AcaoNotificacao): NotificationCompat.Action {
        val intent = Intent(context, SessaoAcaoReceiver::class.java)
            .setAction(SessaoAcaoReceiver.ACTION)
            .putExtra(SessaoAcaoReceiver.EXTRA_ACAO, a.name)
        val pi = PendingIntent.getBroadcast(context, 20 + a.ordinal, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val rotulo = when (a) {
            AcaoNotificacao.RETOMAR -> R.string.acao_retomar
            AcaoNotificacao.INICIAR_PROXIMA -> R.string.acao_iniciar
        }
        return NotificationCompat.Action(0, context.getString(rotulo), pi)
    }
}
