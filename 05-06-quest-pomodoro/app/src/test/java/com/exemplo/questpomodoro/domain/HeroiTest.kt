package com.exemplo.questpomodoro.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HeroiTest {
    @Test fun nivel() {
        assertEquals(1, Heroi.nivel(0))
        assertEquals(1, Heroi.nivel(99))
        assertEquals(2, Heroi.nivel(100))
        assertEquals(3, Heroi.nivel(250))
    }

    @Test fun xpNoNivel() = assertEquals(50, Heroi.xpNoNivel(250))
}
