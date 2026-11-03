package com.exemplo.questpomodoro.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.exemplo.questpomodoro.ui.theme.Cores
import com.exemplo.questpomodoro.ui.theme.TemaRpg
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Caixa de diálogo estilo RPG: cantos retos e borda dupla. */
@Composable
fun PixelPanel(
    modifier: Modifier = Modifier,
    borda: Color = Cores.Borda,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .border(3.dp, borda)
            .padding(3.dp)
            .border(2.dp, borda.copy(alpha = 0.35f))
            .background(Cores.Painel)
            .padding(12.dp),
        content = content,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PixelButton(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cor: Color = Cores.Ouro,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
) {
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    val sombra = 4.dp
    Box(
        modifier
            .defaultMinSize(minHeight = 48.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .drawBehind {
                if (!pressionado) {
                    val s = sombra.toPx()
                    drawRect(Color.Black.copy(alpha = 0.5f), topLeft = Offset(s, s), size = size)
                }
            }
            .offset(if (pressionado) sombra else 0.dp, if (pressionado) sombra else 0.dp)
            .background(cor)
            .border(2.dp, Cores.Fundo)
            .combinedClickable(
                interactionSource = interacao,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            texto.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = Cores.Fundo,
            textAlign = TextAlign.Center,
        )
    }
}

/** Anel de 40 segmentos quadrados preenchidos conforme [progresso] (0..1). */
@Composable
fun TimerCircular(progresso: Float, texto: String, cor: Color, modifier: Modifier = Modifier) {
    val segmentos = 40
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            val raio = min(size.width, size.height) / 2f
            val lado = raio * 0.13f
            val r = raio - lado
            val cheios = (progresso.coerceIn(0f, 1f) * segmentos).toInt()
            for (i in 0 until segmentos) {
                val ang = Math.toRadians(i * 360.0 / segmentos - 90.0)
                val cx = center.x + r * cos(ang).toFloat()
                val cy = center.y + r * sin(ang).toFloat()
                drawRect(
                    color = if (i < cheios) cor else Cores.Painel,
                    topLeft = Offset(cx - lado / 2, cy - lado / 2),
                    size = Size(lado, lado),
                )
            }
        }
        Text(texto, style = MaterialTheme.typography.displaySmall, color = Cores.Borda)
    }
}

/** "NV 3" + barra de 10 blocos. */
@Composable
fun BarraXp(nivel: Int, xpNoNivel: Int, modifier: Modifier = Modifier) {
    Row(modifier.semantics { contentDescription = "Nível $nivel, $xpNoNivel de 100 XP" }, verticalAlignment = Alignment.CenterVertically) {
        Text("NV $nivel", style = MaterialTheme.typography.titleMedium, color = Cores.Ouro)
        Row(Modifier.padding(start = 10.dp).weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            val cheios = (xpNoNivel / 10).coerceIn(0, 10)
            repeat(10) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(12.dp)
                        .background(if (i < cheios) Cores.Verde else Cores.Fundo)
                        .border(1.dp, Cores.Aco),
                )
            }
        }
    }
}

/** Blocos cheios (vencidas) e vazios (restantes). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressoRodadas(concluidas: Int, total: Int, modifier: Modifier = Modifier) {
    FlowRow(
        modifier.semantics { contentDescription = "$concluidas de $total batalhas vencidas" },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(total) { i ->
            Box(
                Modifier
                    .size(12.dp)
                    .background(if (i < concluidas) Cores.Ouro else Cores.Fundo)
                    .border(2.dp, if (i < concluidas) Cores.Laranja else Cores.Aco),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1C2C, widthDp = 360)
@Composable
private fun ComponentesPreview() {
    TemaRpg {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PixelPanel(Modifier.fillMaxWidth()) {
                Text("Estudar Física", style = MaterialTheme.typography.titleMedium)
                ProgressoRodadas(2, 5, Modifier.padding(top = 8.dp))
            }
            BarraXp(3, 50, Modifier.fillMaxWidth())
            PixelButton("Iniciar", onClick = {})
            TimerCircular(0.3f, "17:30", Cores.Vermelho, Modifier.size(200.dp))
        }
    }
}
