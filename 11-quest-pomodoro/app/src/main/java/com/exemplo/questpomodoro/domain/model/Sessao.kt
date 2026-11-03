package com.exemplo.questpomodoro.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Fase { FOCO, PAUSADO, DESCANSO, AGUARDANDO }

/**
 * Sessão pomodoro ativa. Existe no máximo uma (linha única, id = 0).
 * [rodadaAtual] é a batalha em curso (FOCO/PAUSADO) ou a próxima (DESCANSO/AGUARDANDO).
 */
@Entity(tableName = "sessao_ativa")
data class Sessao(
    @PrimaryKey val id: Int = 0,
    val questId: Long,
    val rodadasTotais: Int,
    val fase: Fase,
    val rodadaAtual: Int,
    val faseIniciadaEm: Long,
    val faseTerminaEm: Long?,
    val restanteAoPausarMs: Long?,
    val focoMs: Long,
    val descansoMs: Long,
)

sealed interface EventoSessao {
    data class BatalhaConcluida(val questId: Long, val rodada: Int, val inicio: Long, val fim: Long) : EventoSessao
    data class QuestConcluida(val questId: Long, val fim: Long) : EventoSessao
    data class DescansoTerminou(val questId: Long, val proximaRodada: Int) : EventoSessao
}

data class Transicao(val sessao: Sessao?, val eventos: List<EventoSessao>)
