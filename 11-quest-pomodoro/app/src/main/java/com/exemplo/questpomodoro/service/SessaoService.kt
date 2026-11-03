package com.exemplo.questpomodoro.service

import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.exemplo.questpomodoro.container
import com.exemplo.questpomodoro.notification.IdsNotificacao
import com.exemplo.questpomodoro.notification.Notificador
import com.exemplo.questpomodoro.notification.conteudoContinuo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Mantém a notificação contínua da sessão enquanto ela existir. */
class SessaoService : LifecycleService() {
    private var iniciado = false

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val tipo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, IdsNotificacao.CONTINUA, Notificador.preparando(this), tipo)
        if (!iniciado) {
            iniciado = true
            val c = container
            lifecycleScope.launch {
                c.sessaoRepository.sessao
                    .flatMapLatest { s ->
                        if (s == null) flowOf(null) else c.questRepository.observarQuest(s.questId).map { q -> s to (q?.titulo ?: "") }
                    }
                    .collect { par ->
                        if (par == null) {
                            ServiceCompat.stopForeground(this@SessaoService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        } else {
                            val (s, titulo) = par
                            val n = Notificador.continua(this@SessaoService, conteudoContinuo(s, titulo, c.relogio.agora()))
                            Notificador.postar(this@SessaoService, IdsNotificacao.CONTINUA, n)
                        }
                    }
            }
        }
        return START_STICKY
    }
}
