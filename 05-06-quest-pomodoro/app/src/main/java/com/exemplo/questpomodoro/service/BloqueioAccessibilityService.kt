package com.exemplo.questpomodoro.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.exemplo.questpomodoro.container
import com.exemplo.questpomodoro.domain.deveBloquear
import com.exemplo.questpomodoro.domain.model.Fase
import com.exemplo.questpomodoro.ui.bloqueio.BloqueioActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Observa só qual app vem para a frente; durante o FOCO, abre a tela de bloqueio. */
class BloqueioAccessibilityService : AccessibilityService() {
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var fase: Fase? = null
    @Volatile private var liberados: Set<String> = emptySet()
    @Volatile private var sistema: Set<String> = emptySet()
    private var ultimoBloqueio = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        sistema = sempreLiberados(this)
        val c = container
        escopo.launch { c.sessaoRepository.sessao.collect { fase = it?.fase } }
        escopo.launch { c.config.configuracoes.collect { liberados = it.appsLiberados } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pacote = event.packageName?.toString()
        if (!deveBloquear(pacote, fase, liberados, sistema)) return
        val agora = SystemClock.elapsedRealtime()
        if (agora - ultimoBloqueio < 500) return
        ultimoBloqueio = agora
        startActivity(
            Intent(this, BloqueioActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        escopo.cancel()
        super.onDestroy()
    }
}
