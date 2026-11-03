package com.exemplo.questpomodoro.ui

import com.exemplo.questpomodoro.MainDispatcherRule
import com.exemplo.questpomodoro.domain.ErroQuest
import com.exemplo.questpomodoro.domain.model.Prioridade
import com.exemplo.questpomodoro.domain.model.Quest
import com.exemplo.questpomodoro.fakes.ReposDeTeste
import com.exemplo.questpomodoro.ui.quest.QuestFormViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuestFormViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private fun vm(r: ReposDeTeste, id: Long = 0) = QuestFormViewModel(id, r.questRepo, r.sessaoRepo, r.config)

    @Test fun previaUsaDuracoesDaConfig() = runTest {
        val r = ReposDeTeste()
        r.config.c = r.config.c.copy(focoMin = 30, descansoMin = 10)
        val vm = vm(r)
        vm.mudarRodadas(3)
        advanceUntilIdle()
        assertEquals("3 batalhas = 90 min de foco + 20 min de pousada", vm.uiState.value.previa)
    }

    @Test fun novaQuest_padroes() = runTest {
        val vm = vm(ReposDeTeste())
        advanceUntilIdle()
        assertEquals(4, vm.uiState.value.rodadas)
        assertEquals(Prioridade.MEDIA, vm.uiState.value.prioridade)
        assertFalse(vm.uiState.value.editando)
    }

    @Test fun salvarTituloVazio_mostraErro_naoConclui() = runTest {
        val r = ReposDeTeste()
        val vm = vm(r)
        vm.salvar()
        advanceUntilIdle()
        assertTrue(ErroQuest.TITULO_VAZIO in vm.uiState.value.erros)
        assertFalse(vm.uiState.value.concluido)
        assertTrue(r.questDao.quests.value.isEmpty())
    }

    @Test fun salvarValido_concluido() = runTest {
        val r = ReposDeTeste()
        val vm = vm(r)
        vm.mudarTitulo("Estudar Física")
        vm.mudarPrioridade(Prioridade.ALTA)
        vm.mudarRodadas(5)
        vm.salvar()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.concluido)
        val q = r.questDao.quests.value.single()
        assertEquals("Estudar Física", q.titulo)
        assertEquals(3, q.prioridade)
        assertEquals(5, q.rodadasTotais)
    }

    @Test fun editar_carregaCampos() = runTest {
        val r = ReposDeTeste()
        val id = r.questDao.inserir(Quest(titulo = "A", descricao = "d", prioridade = 1, rodadasTotais = 7, rodadasConcluidas = 2, dataCriacao = 0))
        val vm = vm(r, id)
        advanceUntilIdle()
        val s = vm.uiState.value
        assertEquals("A", s.titulo)
        assertEquals("d", s.descricao)
        assertEquals(Prioridade.BAIXA, s.prioridade)
        assertEquals(7, s.rodadas)
        assertEquals(2, s.rodadasConcluidas)
        assertTrue(s.editando)
        assertFalse(s.somenteLeitura)
    }

    @Test fun editarQuestConcluida_somenteLeitura() = runTest {
        val r = ReposDeTeste()
        val id = r.questDao.inserir(Quest(titulo = "A", rodadasTotais = 2, rodadasConcluidas = 2, concluida = true, dataCriacao = 0))
        val vm = vm(r, id)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.somenteLeitura)
    }

    @Test fun questEmSessao_somenteLeitura() = runTest {
        val r = ReposDeTeste()
        val id = r.questDao.inserir(Quest(titulo = "A", rodadasTotais = 2, dataCriacao = 0))
        r.sessaoRepo.iniciar(id)
        val vm = vm(r, id)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.somenteLeitura)
    }

    @Test fun mudarRodadas_naoDesceAbaixoDasConcluidas() = runTest {
        val r = ReposDeTeste()
        val id = r.questDao.inserir(Quest(titulo = "A", rodadasTotais = 5, rodadasConcluidas = 3, dataCriacao = 0))
        val vm = vm(r, id)
        advanceUntilIdle()
        vm.mudarRodadas(1)
        advanceUntilIdle()
        assertEquals(3, vm.uiState.value.rodadas)
        vm.mudarRodadas(99)
        advanceUntilIdle()
        assertEquals(20, vm.uiState.value.rodadas)
    }

    @Test fun excluir_concluido() = runTest {
        val r = ReposDeTeste()
        val id = r.questDao.inserir(Quest(titulo = "A", rodadasTotais = 2, dataCriacao = 0))
        val vm = vm(r, id)
        advanceUntilIdle()
        vm.excluir()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.concluido)
        assertTrue(r.questDao.quests.value.isEmpty())
    }
}
