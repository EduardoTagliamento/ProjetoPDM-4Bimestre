package com.exemplo.questpomodoro.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.exemplo.questpomodoro.R

object Canais {
    const val CONTINUO = "batalha"
    const val ALERTAS = "alertas"
    const val LEMBRETES = "lembretes"

    fun criar(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CONTINUO, context.getString(R.string.canal_continuo), NotificationManager.IMPORTANCE_LOW).apply {
                    setShowBadge(false)
                },
                NotificationChannel(ALERTAS, context.getString(R.string.canal_alertas), NotificationManager.IMPORTANCE_HIGH).apply {
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 400, 200, 400)
                },
                NotificationChannel(LEMBRETES, context.getString(R.string.canal_lembretes), NotificationManager.IMPORTANCE_DEFAULT),
            ),
        )
    }
}

object IdsNotificacao {
    const val CONTINUA = 1
    const val ALERTA = 2
    const val LEMBRETE = 3
}
