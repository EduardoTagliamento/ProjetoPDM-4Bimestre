package com.exemplo.questpomodoro.data.repository

import com.exemplo.questpomodoro.domain.model.Sessao

/** Alertas sonoros de fim de fase. */
sealed interface Alerta {
    data class FimDoFoco(val titulo: String, val proximaRodada: Int, val total: Int) : Alerta
    data class FimDoDescanso(val titulo: String, val proximaRodada: Int, val total: Int) : Alerta
    data class BatalhaFinal(val titulo: String) : Alerta
}

/** Efeitos colaterais da sessão (alarme, foreground service, notificações). */
interface SessaoEfeitos {
    fun aoMudarSessao(sessao: Sessao?)
    fun alertar(alerta: Alerta)
}

interface Transacionador {
    suspend fun <R> rodar(bloco: suspend () -> R): R
}
