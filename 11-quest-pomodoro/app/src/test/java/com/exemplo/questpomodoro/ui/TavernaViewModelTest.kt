package com.exemplo.questpomodoro.ui

import com.exemplo.questpomodoro.MainDispatcherRule
import com.exemplo.questpomodoro.domain.Filtro
import com.exemplo.questpomodoro.domain.model.Quest
import com.exemplo.questpomodoro.domain.model.Vitoria
import com.exemplo.questpomodoro.fakes.ReposDeTeste
import com.exemplo.questpomodoro.ui.taverna.TavernaViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TavernaViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private fun vm(r: ReposDeTeste) = TavernaViewModel(r.questRepo, r.sessaoRepo, r.heroi)

    @Test fun filtroPendentes_escondeConcluidas() = runTest {
        val r = ReposDeTeste()
        r.questDao.inserir(Quest(titulo = "A", rodadasTotais = 2, dataCriacao = 1))
        r.questDao.inserir(Quest(titulo = "B", rodadasTotais = 2, rodadasConcluidas = 2, concluida = true, dataCriacao = 2))
        val vm = vm(r)
        vm.mudarFiltro(Filtro.PENDENTES)
        advanceUntilIdle()
        assertEquals(listOf("A"), vm.uiState.value.quests.map { it.titulo })
        assertEquals(Filtro.PENDENTES, vm.uiState.value.filtro)
    }

    @Test fun sessaoAtiva_expoeQuestIdETitulo() = runTest {
        val r = ReposDeTeste()
        val id = r.questDao.inserir(Quest(titulo = "Física", rodadasTotais = 2, dataCriacao = 1))
        r.sessaoRepo.iniciar(id)
        val vm = vm(r)
        advanceUntilIdle()
        assertEquals(id, vm.uiState.value.sessaoQuestId)
        assertEquals("Física", vm.uiState.value.sessaoTitulo)
    }

    @Test fun xp250_nivel3_xpNoNivel50() = runTest {
        val vm = vm(ReposDeTeste(xpInicial = 250))
        advanceUntilIdle()
        assertEquals(3, vm.uiState.value.nivel)
        assertEquals(50, vm.uiState.value.xpNoNivel)
    }

    @Test fun iniciarBatalha_comSessaoAtiva_naoChamaAoIniciar() = runTest {
        val r = ReposDeTeste()
        val a = r.questDao.inserir(Quest(titulo = "A", rodadasTotais = 2, dataCriacao = 1))
        val b = r.questDao.inserir(Quest(titulo = "B", rodadasTotais = 2, dataCriacao = 2))
        r.sessaoRepo.iniciar(a)
        val vm = vm(r)
        var chamado = false
        vm.iniciarBatalha(b) { chamado = true }
        advanceUntilIdle()
        assertFalse(chamado)
    }

    @Test fun iniciarBatalha_semSessao_chamaAoIniciar() = runTest {
        val r = ReposDeTeste()
        val a = r.questDao.inserir(Quest(titulo = "A", rodadasTotais = 2, dataCriacao = 1))
        val vm = vm(r)
        var chamado = false
        vm.iniciarBatalha(a) { chamado = true }
        advanceUntilIdle()
        assertTrue(chamado)
    }

    @Test fun vitoriaPendente_apareceEDispensarLimpa() = runTest {
        val r = ReposDeTeste()
        val v = Vitoria(1, "Física", 100, 1, 2)
        r.heroi.registrarVitoria(v)
        val vm = vm(r)
        advanceUntilIdle()
        assertEquals(v, vm.uiState.value.vitoria)
        vm.dispensarVitoria()
        advanceUntilIdle()
        assertNull(vm.uiState.value.vitoria)
    }
}
