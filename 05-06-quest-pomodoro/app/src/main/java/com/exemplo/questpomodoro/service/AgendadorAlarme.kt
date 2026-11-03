package com.exemplo.questpomodoro.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/** Um único alarme: o fim da fase atual. Exato quando permitido; senão, inexato (a UI também sincroniza). */
class AgendadorAlarme(private val context: Context) {
    private val am = context.getSystemService(AlarmManager::class.java)

    fun agendar(terminaEm: Long) {
        val pi = pendente()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, terminaEm, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, terminaEm, pi)
        }
    }

    fun cancelar() = am.cancel(pendente())

    private fun pendente(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, FaseAlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
