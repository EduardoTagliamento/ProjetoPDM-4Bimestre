package com.exemplo.questpomodoro.domain

object Heroi {
    const val XP_POR_BATALHA = 10
    const val BONUS_QUEST = 50
    const val XP_POR_NIVEL = 100

    fun nivel(xp: Int): Int = 1 + xp / XP_POR_NIVEL

    fun xpNoNivel(xp: Int): Int = xp % XP_POR_NIVEL
}
