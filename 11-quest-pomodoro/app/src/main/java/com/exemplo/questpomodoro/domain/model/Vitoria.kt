package com.exemplo.questpomodoro.domain.model

data class Vitoria(
    val questId: Long,
    val titulo: String,
    val xpGanho: Int,
    val nivelAntes: Int,
    val nivelDepois: Int,
)
