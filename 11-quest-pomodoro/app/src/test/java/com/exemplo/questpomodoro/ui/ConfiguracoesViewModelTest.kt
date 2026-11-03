package com.exemplo.questpomodoro.ui

import com.exemplo.questpomodoro.MainDispatcherRule
import com.exemplo.questpomodoro.data.prefs.Configuracoes
import com.exemplo.questpomodoro.fakes.FakeConfiguracoesStore
import com.exemplo.questpomodoro.ui.config.ConfiguracoesViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConfiguracoesViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun vm_focoForaDaFaixa_normaliza() = runTest {
        val store = FakeConfiguracoesStore()
        val vm = ConfiguracoesViewModel(store) {}
        vm.mudarFoco(90)
        advanceUntilIdle()
        assertEquals(60, vm.uiState.value.focoMin)
        vm.mudarDescanso(0)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.descansoMin)
    }

    @Test fun vm_alternarApp_adicionaERemove() = runTest {
        val store = FakeConfiguracoesStore()
        val vm = ConfiguracoesViewModel(store) {}
        vm.alternarApp("com.android.calculator2")
        advanceUntilIdle()
        assertTrue("com.android.calculator2" in vm.uiState.value.appsLiberados)
        vm.alternarApp("com.android.calculator2")
        advanceUntilIdle()
        assertFalse("com.android.calculator2" in vm.uiState.value.appsLiberados)
    }

    @Test fun vm_mudarLembrete_chamaCallback() = runTest {
        val store = FakeConfiguracoesStore()
        var recebido: Configuracoes? = null
        val vm = ConfiguracoesViewModel(store) { recebido = it }
        vm.mudarLembrete(false, 7, 45)
        advanceUntilIdle()
        assertEquals(false, recebido?.lembreteAtivo)
        assertEquals(7, recebido?.lembreteHora)
        assertEquals(45, recebido?.lembreteMinuto)
        assertFalse(vm.uiState.value.lembreteAtivo)
    }
}
