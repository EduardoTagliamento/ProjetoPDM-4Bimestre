package com.exemplo.questpomodoro.domain

import com.exemplo.questpomodoro.domain.model.EventoSessao
import com.exemplo.questpomodoro.domain.model.Fase
import com.exemplo.questpomodoro.domain.model.Quest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SessaoMaquinaTest {
    private val F = 25 * 60_000L
    private val D = 5 * 60_000L
    private val quest = Quest(id = 7, titulo = "Física", rodadasTotais = 5, rodadasConcluidas = 2, dataCriacao = 0)

    @Test fun iniciar_comecaNaProximaRodada() {
        val s = SessaoMaquina.iniciar(quest, 25, 5, 1000)
        assertEquals(Fase.FOCO, s.fase)
        assertEquals(3, s.rodadaAtual)
        assertEquals(1000 + F, s.faseTerminaEm)
        assertEquals(5, s.rodadasTotais)
        assertEquals(F, s.focoMs)
        assertEquals(D, s.descansoMs)
    }

    @Test fun pausar_congelaRestante_eRetomar_reprojetaFim() {
        val p = SessaoMaquina.pausar(SessaoMaquina.iniciar(quest, 25, 5, 0), 60_000)
        assertEquals(Fase.PAUSADO, p.fase)
        assertEquals(F - 60_000, p.restanteAoPausarMs)
        assertNull(p.faseTerminaEm)
        assertEquals(F - 60_000, SessaoMaquina.restanteMs(p, 999_999_999))
        val r = SessaoMaquina.retomar(p, 500_000)
        assertEquals(Fase.FOCO, r.fase)
        assertEquals(500_000 + F - 60_000, r.faseTerminaEm)
        assertNull(r.restanteAoPausarMs)
        assertEquals(0L, r.faseIniciadaEm)
    }

    @Test fun pausar_noDescanso_naoFazNada() {
        val descanso = SessaoMaquina.avancar(SessaoMaquina.iniciar(quest, 25, 5, 0), F).sessao!!
        assertSame(descanso, SessaoMaquina.pausar(descanso, F + 10))
    }

    @Test fun avancar_fimDoFoco_vaiParaDescanso_comRodadaSeguinte() {
        val t = SessaoMaquina.avancar(SessaoMaquina.iniciar(quest, 25, 5, 0), F)
        assertEquals(Fase.DESCANSO, t.sessao!!.fase)
        assertEquals(4, t.sessao!!.rodadaAtual)
        assertEquals(F + D, t.sessao!!.faseTerminaEm)
        assertEquals(listOf(EventoSessao.BatalhaConcluida(7, 3, 0, F)), t.eventos)
    }

    @Test fun avancar_ultimaRodada_concluiQuest_semDescanso() {
        val t = SessaoMaquina.avancar(SessaoMaquina.iniciar(quest.copy(rodadasConcluidas = 4), 25, 5, 0), F + 1)
        assertNull(t.sessao)
        assertEquals(listOf(EventoSessao.BatalhaConcluida(7, 5, 0, F), EventoSessao.QuestConcluida(7, F)), t.eventos)
    }

    @Test fun avancar_focoEDescansoExpirados_emCadeia_paraEmAguardando() {
        val t = SessaoMaquina.avancar(SessaoMaquina.iniciar(quest, 25, 5, 0), F + D + 10)
        assertEquals(Fase.AGUARDANDO, t.sessao!!.fase)
        assertEquals(4, t.sessao!!.rodadaAtual)
        assertNull(t.sessao!!.faseTerminaEm)
        assertEquals(2, t.eventos.size)
        assertEquals(EventoSessao.DescansoTerminou(7, 4), t.eventos.last())
    }

    @Test fun avancar_semExpirar_semEventos() {
        val s = SessaoMaquina.iniciar(quest, 25, 5, 0)
        val t = SessaoMaquina.avancar(s, F - 1)
        assertEquals(s, t.sessao)
        assertTrue(t.eventos.isEmpty())
    }

    @Test fun avancar_pausado_nuncaExpira() {
        val p = SessaoMaquina.pausar(SessaoMaquina.iniciar(quest, 25, 5, 0), 1000)
        val t = SessaoMaquina.avancar(p, 100 * F)
        assertEquals(p, t.sessao)
        assertTrue(t.eventos.isEmpty())
    }

    @Test fun pularDescanso_vaiParaAguardando_eIniciarProxima_comecaFoco() {
        val descanso = SessaoMaquina.avancar(SessaoMaquina.iniciar(quest, 25, 5, 0), F).sessao!!
        val aguardando = SessaoMaquina.pularDescanso(descanso)
        assertEquals(Fase.AGUARDANDO, aguardando.fase)
        assertNull(aguardando.faseTerminaEm)
        val foco = SessaoMaquina.iniciarProxima(aguardando, 9_000_000)
        assertEquals(Fase.FOCO, foco.fase)
        assertEquals(4, foco.rodadaAtual)
        assertEquals(9_000_000L, foco.faseIniciadaEm)
        assertEquals(9_000_000 + F, foco.faseTerminaEm)
    }

    @Test fun iniciarProxima_foraDeAguardando_naoFazNada() {
        val s = SessaoMaquina.iniciar(quest, 25, 5, 0)
        assertSame(s, SessaoMaquina.iniciarProxima(s, 10))
    }

    @Test fun restanteMs_nuncaNegativo() {
        val s = SessaoMaquina.iniciar(quest, 25, 5, 0)
        assertEquals(0L, SessaoMaquina.restanteMs(s, F + 5000))
        assertEquals(F - 1000, SessaoMaquina.restanteMs(s, 1000))
    }
}
