package com.exemplo.questpomodoro.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class LembreteTest {
    @Test fun antesDoHorario_hoje() =
        assertEquals(Duration.ofHours(2), atrasoAteProximo(LocalDateTime.of(2026, 11, 2, 17, 0), 19, 0))

    @Test fun depoisDoHorario_amanha() =
        assertEquals(Duration.ofHours(23), atrasoAteProximo(LocalDateTime.of(2026, 11, 2, 20, 0), 19, 0))

    @Test fun exatamenteNoHorario_amanha() =
        assertEquals(Duration.ofHours(24), atrasoAteProximo(LocalDateTime.of(2026, 11, 2, 19, 0), 19, 0))

    @Test fun comMinutos() =
        assertEquals(Duration.ofMinutes(31), atrasoAteProximo(LocalDateTime.of(2026, 11, 2, 18, 59), 19, 30))

    @Test fun textos() {
        assertNull(textoLembrete(0))
        assertEquals("Sua party te espera: 1 quest pendente.", textoLembrete(1))
        assertEquals("Sua party te espera: 3 quests pendentes.", textoLembrete(3))
    }
}
