package com.exemplo.questpomodoro.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exemplo.questpomodoro.R

/** Paleta Sweetie 16 (GrafxKid, CC0). */
object Cores {
    val Fundo = Color(0xFF1A1C2C)
    val Painel = Color(0xFF333C57)
    val Borda = Color(0xFFF4F4F4)
    val Ouro = Color(0xFFFFCD75)
    val Laranja = Color(0xFFEF7D57)
    val Vermelho = Color(0xFFB13E53)
    val Roxo = Color(0xFF5D275D)
    val Verde = Color(0xFF38B764)
    val Azul = Color(0xFF41A6F6)
    val AzulEscuro = Color(0xFF29366F)
    val Aco = Color(0xFF566C86)
    val TextoSecundario = Color(0xFF94B0C2)
}

val FontePixel = FontFamily(Font(R.font.press_start_2p))

private fun pixel(tamanho: Int, altura: Int = tamanho + 8) =
    TextStyle(fontFamily = FontePixel, fontSize = tamanho.sp, lineHeight = altura.sp)

private val TipografiaRpg = Typography(
    displaySmall = pixel(28, 36),
    headlineMedium = pixel(18, 26),
    headlineSmall = pixel(16, 24),
    titleLarge = pixel(14, 22),
    titleMedium = pixel(12, 20),
    titleSmall = pixel(10, 16),
    labelLarge = pixel(11, 16),
    labelMedium = pixel(9, 14),
    labelSmall = pixel(8, 12),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontSize = 12.sp, lineHeight = 16.sp),
)

private val CoresRpg = darkColorScheme(
    primary = Cores.Ouro,
    onPrimary = Cores.Fundo,
    secondary = Cores.Azul,
    onSecondary = Cores.Fundo,
    tertiary = Cores.Verde,
    background = Cores.Fundo,
    onBackground = Cores.Borda,
    surface = Cores.Painel,
    onSurface = Cores.Borda,
    surfaceVariant = Cores.Painel,
    onSurfaceVariant = Cores.TextoSecundario,
    surfaceContainer = Cores.Painel,
    surfaceContainerHigh = Cores.Painel,
    surfaceContainerHighest = Cores.Painel,
    error = Cores.Vermelho,
    onError = Cores.Borda,
    outline = Cores.Borda,
)

private val FormasRetas = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

@Composable
fun TemaRpg(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CoresRpg, typography = TipografiaRpg, shapes = FormasRetas) {
        // Telas sem Scaffold/Surface herdariam preto; o padrão do app é texto claro.
        CompositionLocalProvider(LocalContentColor provides Cores.Borda, content = content)
    }
}
