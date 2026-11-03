package com.exemplo.questpomodoro.notification

import com.exemplo.questpomodoro.data.repository.Alerta
import com.exemplo.questpomodoro.domain.SessaoMaquina
import com.exemplo.questpomodoro.domain.formatarTempo
import com.exemplo.questpomodoro.domain.model.Fase
import com.exemplo.questpomodoro.domain.model.Sessao

enum class AcaoNotificacao { RETOMAR, INICIAR_PROXIMA }

/** Conteúdo de uma notificação, independente do Android (testável na JVM). */
data class ConteudoNotificacao(
    val titulo: String,
    val texto: String,
    val cronometroAte: Long?,
    val acao: AcaoNotificacao?,
)

fun conteudoContinuo(s: Sessao, tituloQuest: String, agora: Long): ConteudoNotificacao {
    val placar = "${s.rodadaAtual}/${s.rodadasTotais}"
    return when (s.fase) {
        Fase.FOCO -> ConteudoNotificacao(tituloQuest, "⚔️ Batalha $placar", s.faseTerminaEm, null)
        Fase.PAUSADO -> ConteudoNotificacao(
            tituloQuest,
            "⏸ Batalha pausada — ${formatarTempo(SessaoMaquina.restanteMs(s, agora))} restantes",
            null,
            AcaoNotificacao.RETOMAR,
        )
        Fase.DESCANSO -> ConteudoNotificacao(tituloQuest, "🍺 Pousada — próxima: Batalha $placar", s.faseTerminaEm, null)
        Fase.AGUARDANDO -> ConteudoNotificacao(tituloQuest, "Pronto para a Batalha $placar", null, AcaoNotificacao.INICIAR_PROXIMA)
    }
}

fun conteudoAlerta(a: Alerta): ConteudoNotificacao = when (a) {
    is Alerta.FimDoFoco -> ConteudoNotificacao(
        "Hora da Pousada! 🍺",
        "Batalha ${a.proximaRodada - 1}/${a.total} vencida em ${a.titulo}.",
        null,
        null,
    )
    is Alerta.FimDoDescanso -> ConteudoNotificacao(
        "De volta à batalha! ⚔️",
        "${a.titulo} — Batalha ${a.proximaRodada}/${a.total} te espera.",
        null,
        AcaoNotificacao.INICIAR_PROXIMA,
    )
    is Alerta.BatalhaFinal -> ConteudoNotificacao("Vitória! 🏆", "Última batalha de ${a.titulo} vencida.", null, null)
}
