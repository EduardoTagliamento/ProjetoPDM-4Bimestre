package com.exemplo.questpomodoro.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.exemplo.questpomodoro.container
import com.exemplo.questpomodoro.data.prefs.Configuracoes
import com.exemplo.questpomodoro.domain.atrasoAteProximo
import com.exemplo.questpomodoro.domain.textoLembrete
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object LembreteAgendador {
    private const val NOME = "lembrete"

    /** Agenda (ou cancela) o próximo lembrete diário. WorkManager persiste o agendamento após reiniciar. */
    fun reagendar(context: Context, c: Configuracoes, politica: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE) {
        val wm = WorkManager.getInstance(context)
        if (!c.lembreteAtivo) {
            wm.cancelUniqueWork(NOME)
            return
        }
        val atraso = atrasoAteProximo(LocalDateTime.now(), c.lembreteHora, c.lembreteMinuto)
        val pedido = OneTimeWorkRequestBuilder<LembreteWorker>()
            .setInitialDelay(atraso.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniqueWork(NOME, politica, pedido)
    }
}

class LembreteWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        textoLembrete(c.questRepository.contarPendentes())?.let {
            Notificador.postar(applicationContext, IdsNotificacao.LEMBRETE, Notificador.lembrete(applicationContext, it))
        }
        // APPEND: o próximo só roda depois deste terminar (REPLACE cancelaria este worker).
        LembreteAgendador.reagendar(applicationContext, c.config.atual(), ExistingWorkPolicy.APPEND_OR_REPLACE)
        return Result.success()
    }
}
