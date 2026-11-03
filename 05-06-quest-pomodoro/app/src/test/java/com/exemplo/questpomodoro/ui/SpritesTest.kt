package com.exemplo.questpomodoro.ui

import com.exemplo.questpomodoro.ui.components.Sprites
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpritesTest {
    @Test fun todosConsistentes() {
        assertEquals(6, Sprites.todos.size)
        Sprites.todos.forEachIndexed { i, sprite ->
            assertEquals("sprite $i: linhas", 16, sprite.linhas.size)
            sprite.linhas.forEachIndexed { y, linha ->
                assertEquals("sprite $i linha $y: largura", 16, linha.length)
                linha.forEach { ch ->
                    assertTrue("sprite $i linha $y: cor '$ch' sem paleta", ch == '.' || ch in sprite.paleta)
                }
            }
        }
    }
}
