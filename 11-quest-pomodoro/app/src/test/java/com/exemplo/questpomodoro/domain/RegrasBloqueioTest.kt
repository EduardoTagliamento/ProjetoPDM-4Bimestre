package com.exemplo.questpomodoro.domain

import com.exemplo.questpomodoro.domain.model.Fase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegrasBloqueioTest {
    private val proprio = setOf("com.exemplo.questpomodoro")

    @Test fun bloqueiaAppComumNoFoco() =
        assertTrue(deveBloquear("com.instagram.android", Fase.FOCO, emptySet(), proprio))

    @Test fun naoBloqueiaForaDoFoco() {
        listOf(Fase.PAUSADO, Fase.DESCANSO, Fase.AGUARDANDO, null).forEach {
            assertFalse("fase $it", deveBloquear("com.instagram.android", it, emptySet(), emptySet()))
        }
    }

    @Test fun naoBloqueiaLiberadoPeloUsuario() =
        assertFalse(deveBloquear("com.android.calculator2", Fase.FOCO, setOf("com.android.calculator2"), proprio))

    @Test fun naoBloqueiaProprioApp() =
        assertFalse(deveBloquear("com.exemplo.questpomodoro", Fase.FOCO, emptySet(), proprio))

    @Test fun naoBloqueiaTelaDeChamada() {
        listOf("com.android.incallui", "com.google.android.dialer", "com.android.server.telecom").forEach {
            assertFalse(it, deveBloquear(it, Fase.FOCO, emptySet(), PACOTES_SISTEMA_LIBERADOS))
        }
    }

    @Test fun naoBloqueiaSystemUiNemSubpacotes() =
        assertFalse(deveBloquear("com.android.systemui.accessibility", Fase.FOCO, emptySet(), emptySet()))

    @Test fun pacoteNulo_naoBloqueia() = assertFalse(deveBloquear(null, Fase.FOCO, emptySet(), emptySet()))

    @Test fun servicoAtivo_formaCompletaECurta() {
        val c = "com.exemplo.questpomodoro.service.BloqueioAccessibilityService"
        val p = "com.exemplo.questpomodoro"
        assertTrue(servicoAcessibilidadeAtivo("x/y:$p/$c", p, c))
        assertTrue(servicoAcessibilidadeAtivo("$p/.service.BloqueioAccessibilityService", p, c))
        assertTrue(servicoAcessibilidadeAtivo("COM.EXEMPLO.QUESTPOMODORO/$c", p, c))
        assertFalse(servicoAcessibilidadeAtivo(null, p, c))
        assertFalse(servicoAcessibilidadeAtivo("", p, c))
        assertFalse(servicoAcessibilidadeAtivo("$p/.service.Outro", p, c))
    }
}
