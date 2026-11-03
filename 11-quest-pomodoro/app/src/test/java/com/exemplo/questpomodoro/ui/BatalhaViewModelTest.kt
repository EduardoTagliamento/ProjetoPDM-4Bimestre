package com.exemplo.questpomodoro.ui

import com.exemplo.questpomodoro.MainDispatcherRule
import com.exemplo.questpomodoro.domain.model.Fase
import com.exemplo.questpomodoro.domain.model.Quest
import com.exemplo.questpomodoro.fakes.ReposDeTeste
import com.exemplo.questpomodoro.ui.batalha.BatalhaViewModel
import com.exemplo.questpomodoro.ui.batalha.textoConfirmacaoDesistir
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BatalhaViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val F = 25 * 60_000L
    private val tique = MutableSharedFlow<Unit>()

    private suspend fun preparar(): Pair<ReposDeTeste, BatalhaViewModel> {
        val r = ReposDeTeste()
        val id = r.questDao.inserir(Quest(titulo = "Física", rodadasTotais = 5, dataCriacao = 0))
        r.sessaoRepo.iniciar(id)
        return r to BatalhaViewModel(r.sessaoRepo, r.questRepo, r.relogio, tique)
    }

    @Test fun foco_restanteEProgresso() = runTest {
        val (r, vm) = preparar()
        r.relogio.t = 5 * 60_000L
        advanceUntilIdle()
        tique.emit(Unit)
        advanceUntilIdle()
        val s = vm.uiState.value
        assertEquals(Fase.FOCO, s.fase)
        assertEquals("Física", s.titulo)
        assertEquals(1, s.rodadaAtual)
        assertEquals(5, s.rodadasTotais)
        assertEquals(20 * 60_000L, s.restanteMs)
        assertEquals(0.2f, s.progresso, 0.001f)
        assertFalse(s.encerrada)
    }

    @Test fun pausado_restanteCongelado() = runTest {
        val (r, vm) = preparar()
        r.relogio.t = 60_000
        r.sessaoRepo.pausar()
        r.relogio.t = 10 * 60_000L
        advanceUntilIdle()
        tique.emit(Unit)
        advanceUntilIdle()
        assertEquals(Fase.PAUSADO, vm.uiState.value.fase)
        assertEquals(24 * 60_000L, vm.uiState.value.restanteMs)
    }

    @Test fun chegarAZero_sincronizaSemAlarme() = runTest {
        val (r, vm) = preparar()
        advanceUntilIdle()
        r.relogio.t = F
        repeat(3) { tique.emit(Unit); advanceUntilIdle() }
        assertEquals(Fase.DESCANSO, vm.uiState.value.fase)
        assertEquals(2, vm.uiState.value.rodadaAtual)
        assertEquals(1, r.efeitos.alertas.size)
    }

    @Test fun acoes_delegamAoRepositorio() = runTest {
        val (r, vm) = preparar()
        advanceUntilIdle()
        vm.pausar()
        advanceUntilIdle()
        assertEquals(Fase.PAUSADO, vm.uiState.value.fase)
        vm.retomar()
        advanceUntilIdle()
        assertEquals(Fase.FOCO, vm.uiState.value.fase)
        vm.desistir()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.encerrada)
        assertTrue(r.sessaoDao.sessao.value == null)
    }

    @Test fun sessaoNula_encerrada() = runTest {
        val r = ReposDeTeste()
        val vm = BatalhaViewModel(r.sessaoRepo, r.questRepo, r.relogio, tique)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.encerrada)
    }

    @Test fun textoDesistir_porFase() {
        assertEquals("Tem certeza? A batalha atual não vai contar.", textoConfirmacaoDesistir(Fase.FOCO, 3, 5))
        assertEquals("Tem certeza? A batalha atual não vai contar.", textoConfirmacaoDesistir(Fase.PAUSADO, 3, 5))
        assertEquals("Sair da quest? Seu progresso (2/5) fica salvo.", textoConfirmacaoDesistir(Fase.DESCANSO, 3, 5))
        assertEquals("Sair da quest? Seu progresso (2/5) fica salvo.", textoConfirmacaoDesistir(Fase.AGUARDANDO, 3, 5))
    }
}
