package com.exemplo.questpomodoro.domain

import com.exemplo.questpomodoro.domain.model.Quest

enum class ErroQuest { TITULO_VAZIO, TITULO_LONGO, RODADAS_FORA_DO_INTERVALO, RODADAS_ABAIXO_DO_CONCLUIDO }

enum class Filtro { TODAS, PENDENTES, CONCLUIDAS }

enum class Ordem { PRIORIDADE, DATA }

object QuestRegras {
    const val TITULO_MAX = 60
    val RODADAS = 1..20

    fun validar(titulo: String, rodadasTotais: Int, rodadasConcluidas: Int = 0): Set<ErroQuest> = buildSet {
        val limpo = titulo.trim()
        if (limpo.isEmpty()) add(ErroQuest.TITULO_VAZIO)
        if (limpo.length > TITULO_MAX) add(ErroQuest.TITULO_LONGO)
        if (rodadasTotais !in RODADAS) add(ErroQuest.RODADAS_FORA_DO_INTERVALO)
        if (rodadasTotais < rodadasConcluidas) add(ErroQuest.RODADAS_ABAIXO_DO_CONCLUIDO)
    }

    fun textoPrevia(rodadas: Int, focoMin: Int, descansoMin: Int): String {
        val foco = rodadas * focoMin
        if (rodadas <= 1) return "$rodadas batalha = $foco min de foco"
        val descanso = (rodadas - 1) * descansoMin
        return "$rodadas batalhas = $foco min de foco + $descanso min de pousada"
    }

    fun filtrarEOrdenar(quests: List<Quest>, filtro: Filtro, ordem: Ordem): List<Quest> {
        val filtradas = when (filtro) {
            Filtro.TODAS -> quests
            Filtro.PENDENTES -> quests.filter { !it.concluida }
            Filtro.CONCLUIDAS -> quests.filter { it.concluida }
        }
        val porOrdem: Comparator<Quest> = when (ordem) {
            Ordem.PRIORIDADE -> compareByDescending<Quest> { it.prioridade }.thenByDescending { it.dataCriacao }
            Ordem.DATA -> compareByDescending { it.dataCriacao }
        }
        return filtradas.sortedWith(compareBy<Quest> { it.concluida }.then(porOrdem))
    }
}
