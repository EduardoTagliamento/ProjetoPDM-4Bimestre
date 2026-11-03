package com.exemplo.questpomodoro.ui.taverna

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.exemplo.questpomodoro.R
import com.exemplo.questpomodoro.domain.model.Vitoria
import com.exemplo.questpomodoro.ui.components.PixelButton
import com.exemplo.questpomodoro.ui.components.SpritePixel
import com.exemplo.questpomodoro.ui.components.Sprites
import com.exemplo.questpomodoro.ui.theme.Cores

@Composable
fun VitoriaDialog(vitoria: Vitoria, aoFechar: () -> Unit) {
    Dialog(onDismissRequest = aoFechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Cores.Fundo.copy(alpha = 0.96f))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SpritePixel(Sprites.Trofeu, Modifier.size(128.dp))
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.vitoria_titulo), style = MaterialTheme.typography.headlineMedium, color = Cores.Ouro, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(vitoria.titulo, style = MaterialTheme.typography.bodyLarge, color = Cores.Borda, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Text("+${vitoria.xpGanho} XP", style = MaterialTheme.typography.headlineSmall, color = Cores.Verde)
            if (vitoria.nivelDepois > vitoria.nivelAntes) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.vitoria_level_up, vitoria.nivelDepois),
                    style = MaterialTheme.typography.titleLarge,
                    color = Cores.Laranja,
                )
            }
            Spacer(Modifier.height(32.dp))
            PixelButton(stringResource(R.string.vitoria_continuar), onClick = aoFechar, modifier = Modifier.fillMaxWidth(0.7f))
        }
    }
}
