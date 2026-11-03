package com.exemplo.questpomodoro

import android.app.Application
import com.exemplo.questpomodoro.notification.Canais

class QuestApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Canais.criar(this)
    }
}
