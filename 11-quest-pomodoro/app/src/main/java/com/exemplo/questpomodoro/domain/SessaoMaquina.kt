package com.exemplo.questpomodoro.domain

import com.exemplo.questpomodoro.domain.model.EventoSessao
import com.exemplo.questpomodoro.domain.model.Fase
import com.exemplo.questpomodoro.domain.model.Quest
import com.exemplo.questpomodoro.domain.model.Sessao
import com.exemplo.questpomodoro.domain.model.Transicao

/** Regras puras da sessão. O tempo restante sempre deriva de timestamps. */
object SessaoMaquina {

    fun iniciar(quest: Quest, focoMin: Int, descansoMin: Int, agora: Long): Sessao {
        val focoMs = focoMin * 60_000L
        return Sessao(
            questId = quest.id,
            rodadasTotais = quest.rodadasTotais,
            fase = Fase.FOCO,
            rodadaAtual = quest.rodadasConcluidas + 1,
            faseIniciadaEm = agora,
            faseTerminaEm = agora + focoMs,
            restanteAoPausarMs = null,
            focoMs = focoMs,
            descansoMs = descansoMin * 60_000L,
        )
    }

    fun pausar(s: Sessao, agora: Long): Sessao {
        if (s.fase != Fase.FOCO) return s
        return s.copy(fase = Fase.PAUSADO, restanteAoPausarMs = restanteMs(s, agora), faseTerminaEm = null)
    }

    fun retomar(s: Sessao, agora: Long): Sessao {
        if (s.fase != Fase.PAUSADO) return s
        return s.copy(fase = Fase.FOCO, faseTerminaEm = agora + (s.restanteAoPausarMs ?: 0L), restanteAoPausarMs = null)
    }

    fun pularDescanso(s: Sessao): Sessao {
        if (s.fase != Fase.DESCANSO) return s
        return s.copy(fase = Fase.AGUARDANDO, faseTerminaEm = null)
    }

    fun iniciarProxima(s: Sessao, agora: Long): Sessao {
        if (s.fase != Fase.AGUARDANDO) return s
        return s.copy(fase = Fase.FOCO, faseIniciadaEm = agora, faseTerminaEm = agora + s.focoMs, restanteAoPausarMs = null)
    }

    fun avancar(s: Sessao, agora: Long): Transicao {
        val eventos = mutableListOf<EventoSessao>()
        var atual: Sessao? = s
        while (true) {
            val sessao = atual ?: break
            val fim = sessao.faseTerminaEm ?: break
            if (agora < fim) break
            atual = when (sessao.fase) {
                Fase.FOCO -> {
                    eventos += EventoSessao.BatalhaConcluida(sessao.questId, sessao.rodadaAtual, sessao.faseIniciadaEm, fim)
                    if (sessao.rodadaAtual >= sessao.rodadasTotais) {
                        eventos += EventoSessao.QuestConcluida(sessao.questId, fim)
                        null
                    } else {
                        sessao.copy(
                            fase = Fase.DESCANSO,
                            rodadaAtual = sessao.rodadaAtual + 1,
                            faseIniciadaEm = fim,
                            faseTerminaEm = fim + sessao.descansoMs,
                        )
                    }
                }
                Fase.DESCANSO -> {
                    eventos += EventoSessao.DescansoTerminou(sessao.questId, sessao.rodadaAtual)
                    sessao.copy(fase = Fase.AGUARDANDO, faseTerminaEm = null)
                }
                Fase.PAUSADO, Fase.AGUARDANDO -> break
            }
        }
        return Transicao(atual, eventos)
    }

    fun restanteMs(s: Sessao, agora: Long): Long = when (s.fase) {
        Fase.PAUSADO -> s.restanteAoPausarMs ?: 0L
        Fase.AGUARDANDO -> 0L
        Fase.FOCO, Fase.DESCANSO -> ((s.faseTerminaEm ?: agora) - agora).coerceAtLeast(0L)
    }
}
