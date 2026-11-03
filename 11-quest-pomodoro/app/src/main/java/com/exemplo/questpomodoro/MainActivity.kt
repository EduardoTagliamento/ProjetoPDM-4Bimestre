package com.exemplo.questpomodoro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.exemplo.questpomodoro.notification.Notificador
import com.exemplo.questpomodoro.ui.Navegacao
import com.exemplo.questpomodoro.ui.theme.TemaRpg
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var destino by mutableStateOf<String?>(null)
    private var pedido by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) lerDestino(intent)
        setContent {
            TemaRpg { Navegacao(destino, pedido) }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch { container.restaurarSessao() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        lerDestino(intent)
    }

    private fun lerDestino(intent: Intent?) {
        destino = intent?.getStringExtra(Notificador.EXTRA_DESTINO)
        pedido++
    }
}
