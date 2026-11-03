package com.exemplo.questpomodoro.domain.model

enum class Prioridade(val valor: Int, val raridade: String) {
    BAIXA(1, "Comum"),
    MEDIA(2, "Rara"),
    ALTA(3, "Épica");

    companion object {
        fun de(valor: Int): Prioridade = entries.firstOrNull { it.valor == valor } ?: MEDIA
    }
}
