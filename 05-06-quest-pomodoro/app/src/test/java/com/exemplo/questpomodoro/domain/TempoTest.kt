package com.exemplo.questpomodoro.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TempoTest {
    @Test fun formatar() {
        assertEquals("25:00", formatarTempo(25 * 60_000L))
        assertEquals("00:01", formatarTempo(1))
        assertEquals("00:00", formatarTempo(0))
        assertEquals("12:40", formatarTempo(760_000))
    }
}
