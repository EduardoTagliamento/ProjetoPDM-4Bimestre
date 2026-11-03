package com.exemplo.questpomodoro.data

import com.exemplo.questpomodoro.data.repository.QuestRepository
import com.exemplo.questpomodoro.data.repository.ResultadoSalvar
import com.exemplo.questpomodoro.domain.ErroQuest
import com.exemplo.questpomodoro.domain.model.Fase
import com.exemplo.questpomodoro.domain.model.Quest
import com.exemplo.questpomodoro.domain.model.Sessao
import com.exemplo.questpomodoro.fakes.FakeQuestDao
import com.exemplo.questpomodoro.fakes.FakeRelogio
import com.exemplo.questpomodoro.fakes.FakeSessaoDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestRepositoryTest {
    private val questDao = FakeQuestDao()
    private val sessaoDao = FakeSessaoDao()
    private val relogio = FakeRelogio(42)
    private val repo = QuestRepository(questDao, sessaoDao, relogio)

    private suspend fun sessaoEm(questId: Long) = sessaoDao.salvar(
        Sessao(questId = questId, rodadasTotais = 5, fase = Fase.FOCO, rodadaAtual = 1, faseIniciadaEm = 0,
            faseTerminaEm = 10, restanteAoPausarMs = null, focoMs = 10, descansoMs = 5),
    )

    @Test fun salvar_tituloEspacos_invalido() = runTest {
        assertEquals(ResultadoSalvar.Invalido(setOf(ErroQuest.TITULO_VAZIO)), repo.salvar(0, "   ", "", 2, 5))
        assertTrue(questDao.quests.value.isEmpty())
    }

    @Test fun salvar_nova_insereComTrimEDataCriacao() = runTest {
        val r = repo.salvar(0, "  Física  ", "cap 3", 3, 5)
        assertTrue(r is ResultadoSalvar.Sucesso)
        val q = questDao.quests.value.single()
        assertEquals((r as ResultadoSalvar.Sucesso).id, q.id)
        assertEquals("Física", q.titulo)
        assertEquals("cap 3", q.descricao)
        assertEquals(3, q.prioridade)
        assertEquals(5, q.rodadasTotais)
        assertEquals(42L, q.dataCriacao)
    }

    @Test fun salvar_edicao_mantemProgressoEData() = runTest {
        val id = questDao.inserir(Quest(titulo = "A", rodadasTotais = 5, rodadasConcluidas = 2, dataCriacao = 7))
        assertEquals(ResultadoSalvar.Sucesso(id), repo.salvar(id, "B", "", 1, 4))
        val q = questDao.quests.value.single()
        assertEquals("B", q.titulo)
        assertEquals(4, q.rodadasTotais)
        assertEquals(2, q.rodadasConcluidas)
        assertEquals(7L, q.dataCriacao)
    }

    @Test fun salvar_rodadasAbaixoDoConcluido_invalido() = runTest {
        val id = questDao.inserir(Quest(titulo = "A", rodadasTotais = 5, rodadasConcluidas = 3, dataCriacao = 0))
        val r = repo.salvar(id, "A", "", 2, 2)
        assertEquals(ResultadoSalvar.Invalido(setOf(ErroQuest.RODADAS_ABAIXO_DO_CONCLUIDO)), r)
    }

    @Test fun salvar_questDaSessaoAtiva_bloqueada() = runTest {
        val id = questDao.inserir(Quest(titulo = "A", rodadasTotais = 5, dataCriacao = 0))
        sessaoEm(id)
        assertEquals(ResultadoSalvar.Bloqueada, repo.salvar(id, "Outro", "", 2, 5))
        assertEquals("A", questDao.quests.value.single().titulo)
    }

    @Test fun excluir_questDaSessaoAtiva_retornaFalse_eNaoExclui() = runTest {
        val id = questDao.inserir(Quest(titulo = "A", rodadasTotais = 5, dataCriacao = 0))
        sessaoEm(id)
        assertFalse(repo.excluir(id))
        assertEquals(1, questDao.quests.value.size)
    }

    @Test fun excluir_semSessao_exclui() = runTest {
        val id = questDao.inserir(Quest(titulo = "A", rodadasTotais = 5, dataCriacao = 0))
        assertTrue(repo.excluir(id))
        assertTrue(questDao.quests.value.isEmpty())
    }

    @Test fun salvar_questConcluida_bloqueada() = runTest {
        val id = questDao.inserir(Quest(titulo = "A", rodadasTotais = 2, rodadasConcluidas = 2, concluida = true, dataCriacao = 0))
        assertEquals(ResultadoSalvar.Bloqueada, repo.salvar(id, "B", "", 2, 5))
    }
}
