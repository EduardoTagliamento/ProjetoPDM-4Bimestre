package com.exemplo.questpomodoro

import android.app.Application
import androidx.work.ExistingWorkPolicy
import com.exemplo.questpomodoro.notification.Canais
import com.exemplo.questpomodoro.notification.LembreteAgendador
import kotlinx.coroutines.launch

class QuestApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Canais.criar(this)
        container.escopo.launch {
            LembreteAgendador.reagendar(this@QuestApp, container.config.atual(), ExistingWorkPolicy.KEEP)
        }
    }
}
