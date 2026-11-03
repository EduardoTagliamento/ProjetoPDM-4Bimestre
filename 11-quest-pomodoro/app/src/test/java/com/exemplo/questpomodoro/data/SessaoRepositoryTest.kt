package com.exemplo.questpomodoro.data

import com.exemplo.questpomodoro.data.repository.Alerta
import com.exemplo.questpomodoro.data.repository.SessaoRepository
import com.exemplo.questpomodoro.domain.model.Fase
import com.exemplo.questpomodoro.domain.model.Quest
import com.exemplo.questpomodoro.domain.model.Vitoria
import com.exemplo.questpomodoro.fakes.FakeBatalhaDao
import com.exemplo.questpomodoro.fakes.FakeConfiguracoesStore
import com.exemplo.questpomodoro.fakes.FakeEfeitos
import com.exemplo.questpomodoro.fakes.FakeHeroiStore
import com.exemplo.questpomodoro.fakes.FakeQuestDao
import com.exemplo.questpomodoro.fakes.FakeRelogio
import com.exemplo.questpomodoro.fakes.FakeSessaoDao
import com.exemplo.questpomodoro.fakes.TransacionadorDireto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessaoRepositoryTest {
    private val F = 25 * 60_000L
    private val D = 5 * 60_000L

    private val questDao = FakeQuestDao()
    private val batalhaDao = FakeBatalhaDao()
    private val sessaoDao = FakeSessaoDao()
    private val heroi = FakeHeroiStore()
    private val config = FakeConfiguracoesStore()
    private val relogio = FakeRelogio(0)
    private val efeitos = FakeEfeitos()
    private val repo = SessaoRepository(questDao, batalhaDao, sessaoDao, heroi, config, TransacionadorDireto, relogio, efeitos)

    private suspend fun criarQuest(concluidas: Int = 0, total: Int = 5, concluida: Boolean = false): Long =
        questDao.inserir(Quest(titulo = "Física", rodadasTotais = total, rodadasConcluidas = concluidas, concluida = concluida, dataCriacao = 0))

    private fun quest(id: Long) = questDao.quests.value.first { it.id == id }

    @Test fun iniciar_criaFoco_eNotificaEfeitos() = runTest {
        val id = criarQuest()
        assertTrue(repo.iniciar(id))
        val s = sessaoDao.sessao.value!!
        assertEquals(Fase.FOCO, s.fase)
        assertEquals(1, s.rodadaAtual)
        assertEquals(F, s.faseTerminaEm)
        assertEquals(listOf(s), efeitos.sessoes)
    }

    @Test fun iniciar_duasVezes_segundaRetornaFalse_eSessaoNaoMuda() = runTest {
        val id = criarQuest()
        val outra = criarQuest()
        assertTrue(repo.iniciar(id))
        val antes = sessaoDao.sessao.value
        relogio.t = 1000
        assertFalse(repo.iniciar(id))
        assertFalse(repo.iniciar(outra))
        assertEquals(antes, sessaoDao.sessao.value)
    }

    @Test fun iniciar_concorrente_criaUmaSessaoSo() = runTest {
        val id = criarQuest()
        val resultados = listOf(async { repo.iniciar(id) }, async { repo.iniciar(id) }).awaitAll()
        assertEquals(1, resultados.count { it })
        assertEquals(1, efeitos.sessoes.size)
    }

    @Test fun iniciar_questConcluida_retornaFalse() = runTest {
        val id = criarQuest(concluidas = 5, concluida = true)
        assertFalse(repo.iniciar(id))
        assertNull(sessaoDao.sessao.value)
    }

    @Test fun iniciar_questInexistente_retornaFalse() = runTest {
        assertFalse(repo.iniciar(999))
    }

    @Test fun sincronizar_fimDoFoco_contaRodada_registra_daXp_alertaFimDoFoco() = runTest {
        val id = criarQuest()
        repo.iniciar(id)
        relogio.t = F
        repo.sincronizar()
        assertEquals(1, quest(id).rodadasConcluidas)
        assertEquals(1, batalhaDao.registros.size)
        assertTrue(batalhaDao.registros.single().concluida)
        assertEquals(0L, batalhaDao.registros.single().inicio)
        assertEquals(F, batalhaDao.registros.single().fim)
        assertEquals(10, heroi.xp.value)
        assertEquals(listOf<Alerta>(Alerta.FimDoFoco("Física", 2, 5)), efeitos.alertas)
        assertEquals(Fase.DESCANSO, sessaoDao.sessao.value!!.fase)
        assertEquals(sessaoDao.sessao.value, efeitos.sessoes.last())
    }

    @Test fun sincronizar_ultimaRodada_concluiQuest_vitoria_alertaBatalhaFinal() = runTest {
        val id = criarQuest(concluidas = 4)
        repo.iniciar(id)
        relogio.t = F
        repo.sincronizar()
        val q = quest(id)
        assertTrue(q.concluida)
        assertEquals(5, q.rodadasConcluidas)
        assertEquals(F, q.dataConclusao)
        assertEquals(60, heroi.xp.value)
        assertEquals(Vitoria(id, "Física", 100, 1, 1), heroi.vitoriaPendente.value)
        assertNull(sessaoDao.sessao.value)
        assertNull(efeitos.sessoes.last())
        assertEquals(listOf<Alerta>(Alerta.BatalhaFinal("Física")), efeitos.alertas)
    }

    @Test fun sincronizar_vitoriaComLevelUp() = runTest {
        heroi.xp.value = 90
        val id = criarQuest(concluidas = 4)
        repo.iniciar(id)
        relogio.t = F
        repo.sincronizar()
        assertEquals(Vitoria(id, "Física", 100, 1, 2), heroi.vitoriaPendente.value)
    }

    @Test fun sincronizar_concorrente_aplicaUmaVez() = runTest {
        val id = criarQuest()
        repo.iniciar(id)
        relogio.t = F
        listOf(async { repo.sincronizar() }, async { repo.sincronizar() }).awaitAll()
        assertEquals(1, quest(id).rodadasConcluidas)
        assertEquals(10, heroi.xp.value)
        assertEquals(1, efeitos.alertas.size)
        assertEquals(1, batalhaDao.registros.size)
    }

    @Test fun sincronizar_semExpirar_semAlerta_semMudanca() = runTest {
        val id = criarQuest()
        repo.iniciar(id)
        relogio.t = F - 1
        repo.sincronizar()
        assertTrue(efeitos.alertas.isEmpty())
        assertEquals(1, efeitos.sessoes.size)
        assertEquals(0, quest(id).rodadasConcluidas)
    }

    @Test fun sincronizar_focoEDescansoExpirados_umAlertaSo_FimDoDescanso() = runTest {
        val id = criarQuest()
        repo.iniciar(id)
        relogio.t = F + D + 10
        repo.sincronizar()
        assertEquals(listOf<Alerta>(Alerta.FimDoDescanso("Física", 2, 5)), efeitos.alertas)
        assertEquals(Fase.AGUARDANDO, sessaoDao.sessao.value!!.fase)
        assertEquals(1, quest(id).rodadasConcluidas)
    }

    @Test fun desistir_noFoco_registraNaoConcluida_mantemProgresso() = runTest {
        val id = criarQuest(concluidas = 2)
        repo.iniciar(id)
        relogio.t = 60_000
        repo.desistir()
        assertNull(sessaoDao.sessao.value)
        assertEquals(2, quest(id).rodadasConcluidas)
        val r = batalhaDao.registros.single()
        assertFalse(r.concluida)
        assertEquals(0L, r.inicio)
        assertEquals(60_000L, r.fim)
        assertEquals(0, heroi.xp.value)
        assertNull(efeitos.sessoes.last())
    }

    @Test fun desistir_pausado_registraNaoConcluida() = runTest {
        val id = criarQuest()
        repo.iniciar(id)
        relogio.t = 1000
        repo.pausar()
        relogio.t = 5000
        repo.desistir()
        assertFalse(batalhaDao.registros.single().concluida)
    }

    @Test fun desistir_noDescanso_naoRegistraBatalha() = runTest {
        val id = criarQuest()
        repo.iniciar(id)
        relogio.t = F
        repo.sincronizar()
        batalhaDao.registros.clear()
        relogio.t = F + 1000
        repo.desistir()
        assertTrue(batalhaDao.registros.isEmpty())
        assertNull(sessaoDao.sessao.value)
        assertEquals(1, quest(id).rodadasConcluidas)
    }

    @Test fun configMudadaDuranteSessao_naoAfetaSessaoAtual() = runTest {
        val id = criarQuest()
        repo.iniciar(id)
        config.c = config.c.copy(focoMin = 50)
        relogio.t = F
        repo.sincronizar()
        assertEquals(Fase.DESCANSO, sessaoDao.sessao.value!!.fase)
    }

    @Test fun novaSessao_usaConfigNova() = runTest {
        config.c = config.c.copy(focoMin = 50)
        val id = criarQuest()
        repo.iniciar(id)
        assertEquals(50 * 60_000L, sessaoDao.sessao.value!!.faseTerminaEm)
    }

    @Test fun pausar_depoisDoFimDoFoco_primeiroAvanca() = runTest {
        val id = criarQuest()
        repo.iniciar(id)
        relogio.t = F + 1
        repo.pausar()
        assertEquals(Fase.DESCANSO, sessaoDao.sessao.value!!.fase)
        assertEquals(1, quest(id).rodadasConcluidas)
    }

    @Test fun pausarRetomar_pularDescanso_iniciarProxima() = runTest {
        val id = criarQuest()
        repo.iniciar(id)
        relogio.t = 1000
        repo.pausar()
        assertEquals(Fase.PAUSADO, sessaoDao.sessao.value!!.fase)
        relogio.t = 100_000
        repo.retomar()
        assertEquals(100_000 + F - 1000, sessaoDao.sessao.value!!.faseTerminaEm)
        relogio.t = sessaoDao.sessao.value!!.faseTerminaEm!!
        repo.sincronizar()
        repo.pularDescanso()
        assertEquals(Fase.AGUARDANDO, sessaoDao.sessao.value!!.fase)
        relogio.t += 5000
        repo.iniciarProxima()
        assertEquals(Fase.FOCO, sessaoDao.sessao.value!!.fase)
        assertEquals(2, sessaoDao.sessao.value!!.rodadaAtual)
    }
}
