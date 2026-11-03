package com.exemplo.questpomodoro.domain

import java.time.Duration
import java.time.LocalDateTime

/** Tempo até o próximo [hora]:[minuto] estritamente depois de [agora] (hoje ou amanhã). */
fun atrasoAteProximo(agora: LocalDateTime, hora: Int, minuto: Int): Duration {
    var alvo = agora.toLocalDate().atTime(hora, minuto)
    if (!alvo.isAfter(agora)) alvo = alvo.plusDays(1)
    return Duration.between(agora, alvo)
}

fun textoLembrete(pendentes: Int): String? = when {
    pendentes <= 0 -> null
    pendentes == 1 -> "Sua party te espera: 1 quest pendente."
    else -> "Sua party te espera: $pendentes quests pendentes."
}
