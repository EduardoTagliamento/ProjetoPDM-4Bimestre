package com.exemplo.questpomodoro.data

import com.exemplo.questpomodoro.data.prefs.Configuracoes
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfiguracoesTest {
    @Test fun normalizada_limitaFaixas() {
        val c = Configuracoes(focoMin = 0, descansoMin = 99, lembreteHora = 30, lembreteMinuto = -5).normalizada()
        assertEquals(1, c.focoMin)
        assertEquals(30, c.descansoMin)
        assertEquals(23, c.lembreteHora)
        assertEquals(0, c.lembreteMinuto)
    }

    @Test fun padrao_jaNormalizado() = assertEquals(Configuracoes(), Configuracoes().normalizada())

    @Test fun padroesDaSpec() {
        val c = Configuracoes()
        assertEquals(25, c.focoMin)
        assertEquals(5, c.descansoMin)
        assertEquals(true, c.lembreteAtivo)
        assertEquals(19, c.lembreteHora)
        assertEquals(0, c.lembreteMinuto)
    }
}
