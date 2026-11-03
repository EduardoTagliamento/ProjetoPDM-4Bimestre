package com.exemplo.questpomodoro.notification

import com.exemplo.questpomodoro.data.repository.Alerta
import com.exemplo.questpomodoro.domain.model.Fase
import com.exemplo.questpomodoro.domain.model.Sessao
import org.junit.Assert.assertEquals
import org.junit.Test

class ConteudoNotificacaoTest {
    private val base = Sessao(
        questId = 1, rodadasTotais = 5, fase = Fase.FOCO, rodadaAtual = 2, faseIniciadaEm = 0,
        faseTerminaEm = 1_500_000, restanteAoPausarMs = null, focoMs = 1_500_000, descansoMs = 300_000,
    )

    @Test fun foco() = assertEquals(
        ConteudoNotificacao("Física", "⚔️ Batalha 2/5", 1_500_000, null),
        conteudoContinuo(base, "Física", 0),
    )

    @Test fun pausado() = assertEquals(
        ConteudoNotificacao("Física", "⏸ Batalha pausada — 12:40 restantes", null, AcaoNotificacao.RETOMAR),
        conteudoContinuo(base.copy(fase = Fase.PAUSADO, faseTerminaEm = null, restanteAoPausarMs = 760_000), "Física", 0),
    )

    @Test fun descanso() = assertEquals(
        ConteudoNotificacao("Física", "🍺 Pousada — próxima: Batalha 3/5", 1_800_000, null),
        conteudoContinuo(base.copy(fase = Fase.DESCANSO, rodadaAtual = 3, faseTerminaEm = 1_800_000), "Física", 0),
    )

    @Test fun aguardando() = assertEquals(
        ConteudoNotificacao("Física", "Pronto para a Batalha 3/5", null, AcaoNotificacao.INICIAR_PROXIMA),
        conteudoContinuo(base.copy(fase = Fase.AGUARDANDO, rodadaAtual = 3, faseTerminaEm = null), "Física", 0),
    )

    @Test fun alertaFimDoFoco() = assertEquals(
        ConteudoNotificacao("Hora da Pousada! 🍺", "Batalha 2/5 vencida em Física.", null, null),
        conteudoAlerta(Alerta.FimDoFoco("Física", 3, 5)),
    )

    @Test fun alertaFimDoDescanso() = assertEquals(
        ConteudoNotificacao("De volta à batalha! ⚔️", "Física — Batalha 3/5 te espera.", null, AcaoNotificacao.INICIAR_PROXIMA),
        conteudoAlerta(Alerta.FimDoDescanso("Física", 3, 5)),
    )

    @Test fun alertaBatalhaFinal() = assertEquals(
        ConteudoNotificacao("Vitória! 🏆", "Última batalha de Física vencida.", null, null),
        conteudoAlerta(Alerta.BatalhaFinal("Física")),
    )
}
