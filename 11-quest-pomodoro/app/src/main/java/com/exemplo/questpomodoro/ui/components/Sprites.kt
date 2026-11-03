package com.exemplo.questpomodoro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.min

/** Sprite em grade de caracteres; '.' é transparente. */
data class Sprite(val linhas: List<String>, val paleta: Map<Char, Color>)

@Composable
fun SpritePixel(sprite: Sprite, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val linhas = sprite.linhas.size
        val colunas = sprite.linhas.maxOf { it.length }
        val px = floor(min(size.width / colunas, size.height / linhas))
        val origem = Offset((size.width - px * colunas) / 2f, (size.height - px * linhas) / 2f)
        sprite.linhas.forEachIndexed { y, linha ->
            linha.forEachIndexed { x, ch ->
                val cor = sprite.paleta[ch] ?: return@forEachIndexed
                drawRect(cor, topLeft = origem + Offset(x * px, y * px), size = Size(px + 0.5f, px + 0.5f))
            }
        }
    }
}

/** Sprites originais 16×16, paleta Sweetie 16. */
object Sprites {
    private val contorno = Color(0xFF29366F)
    private val aco = Color(0xFF94B0C2)
    private val branco = Color(0xFFF4F4F4)
    private val ouro = Color(0xFFFFCD75)
    private val laranja = Color(0xFFEF7D57)
    private val vermelho = Color(0xFFB13E53)
    private val roxo = Color(0xFF5D275D)
    private val azul = Color(0xFF3B5DC9)
    private val cinza = Color(0xFF566C86)
    private val escuro = Color(0xFF333C57)

    val Heroi = Sprite(
        listOf(
            "......kkkk......",
            ".....kssssk.....",
            "....kssssssk....",
            "....kskkkksk....",
            "....ksppppsk....",
            "....kspkpksk....",
            ".....kppppk.....",
            "....kkrrrrkk....",
            "...krrrggrrrk...",
            "..kprrrggrrrpk..",
            "..kprrrrrrrrpk..",
            "...kkbbbbbbkk...",
            "....kssk.kssk...",
            "....kssk.kssk...",
            "...kkkkk.kkkkk..",
            "................",
        ),
        mapOf('k' to contorno, 's' to aco, 'p' to ouro, 'r' to vermelho, 'g' to laranja, 'b' to roxo),
    )

    val Espada = Sprite(
        listOf(
            ".............kk.",
            "............kwsk",
            "...........kwsk.",
            "..........kwsk..",
            ".........kwsk...",
            "........kwsk....",
            ".......kwsk.....",
            "......kwsk......",
            ".kk..kwsk.......",
            ".kgkkwsk........",
            "..kggsk.........",
            "...kggk.........",
            "..kbkggk........",
            ".kbk..kk........",
            "kbk.............",
            "kk..............",
        ),
        mapOf('k' to contorno, 'w' to branco, 's' to aco, 'g' to ouro, 'b' to roxo),
    )

    val Fogueira = Sprite(
        listOf(
            "................",
            ".......y........",
            "......yy........",
            "......yoy.......",
            ".....yooy..y....",
            "....yoooy.yy....",
            "....yorooyoy....",
            "...yorrrooooy...",
            "...yorrrrroooy..",
            "....yorrrrrooy..",
            ".....yooooooy...",
            "..bbb.yyyyy.bbb.",
            "...bbbbbbbbbb...",
            "..bbdbbbbbbdbb..",
            ".bbb........bbb.",
            "................",
        ),
        mapOf('y' to ouro, 'o' to laranja, 'r' to vermelho, 'b' to roxo, 'd' to escuro),
    )

    val Caneca = Sprite(
        listOf(
            "................",
            "................",
            "...wwwwwww......",
            "..wwwwwwwww.....",
            "..kwwwwwwwk.....",
            "..kyyyyyyyk.....",
            "..kyyyyyyykkkk..",
            "..kyoyyyyyk..k..",
            "..kyoyyyyyk..k..",
            "..kyoyyyyyk..k..",
            "..kyyyyyyykkkk..",
            "..kyyyyyyyk.....",
            "..kyyyyyyyk.....",
            "..kkkkkkkkk.....",
            "................",
            "................",
        ),
        mapOf('w' to branco, 'y' to ouro, 'o' to branco, 'k' to cinza),
    )

    val Trofeu = Sprite(
        listOf(
            "................",
            "...kkkkkkkkkk...",
            ".kkkyyyyyyyykkk.",
            ".k.kywyyyyyyk.k.",
            ".k.kywyyyyyyk.k.",
            "..kkyyyyyyyykk..",
            "....kyyyyyyk....",
            ".....kyyyyk.....",
            "......kyyk......",
            ".......kk.......",
            ".......yy.......",
            "......kyyk......",
            ".....kyyyyk.....",
            "....kbbbbbbk....",
            "....kbbbbbbk....",
            "....kkkkkkkk....",
        ),
        mapOf('k' to laranja, 'y' to ouro, 'w' to branco, 'b' to vermelho),
    )

    val Escudo = Sprite(
        listOf(
            "................",
            "..kkkkkkkkkkkk..",
            "..kssssssssssk..",
            "..ksbbbbbbbbsk..",
            "..ksbbbggbbbsk..",
            "..ksbbbggbbbsk..",
            "..ksbggggggbsk..",
            "..ksbggggggbsk..",
            "..ksbbbggbbbsk..",
            "...ksbbggbbsk...",
            "...ksbbggbbsk...",
            "....ksbbbbsk....",
            ".....ksbbsk.....",
            "......kssk......",
            ".......kk.......",
            "................",
        ),
        mapOf('k' to contorno, 's' to aco, 'b' to azul, 'g' to ouro),
    )

    val todos: List<Sprite> = listOf(Heroi, Espada, Fogueira, Caneca, Trofeu, Escudo)
}

@Preview(backgroundColor = 0xFF1A1C2C, showBackground = true)
@Composable
private fun SpritesPreview() {
    androidx.compose.foundation.layout.Row {
        Sprites.todos.forEach { SpritePixel(it, Modifier.size(48.dp)) }
    }
}
