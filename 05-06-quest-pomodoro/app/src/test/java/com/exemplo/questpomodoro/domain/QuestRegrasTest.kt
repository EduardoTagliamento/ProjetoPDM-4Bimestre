package com.exemplo.questpomodoro.domain

import com.exemplo.questpomodoro.domain.model.Prioridade
import com.exemplo.questpomodoro.domain.model.Quest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestRegrasTest {

    private val altaPendenteAntiga = Quest(id = 1, titulo = "A", prioridade = Prioridade.ALTA.valor, rodadasTotais = 3, dataCriacao = 100)
    private val baixaPendenteNova = Quest(id = 2, titulo = "B", prioridade = Prioridade.BAIXA.valor, rodadasTotais = 3, dataCriacao = 300)
    private val mediaConcluida = Quest(id = 3, titulo = "C", prioridade = Prioridade.MEDIA.valor, rodadasTotais = 3, rodadasConcluidas = 3, concluida = true, dataCriacao = 200)
    private val todas = listOf(mediaConcluida, baixaPendenteNova, altaPendenteAntiga)

    @Test fun tituloSoComEspacos_eVazio() = assertEquals(setOf(ErroQuest.TITULO_VAZIO), QuestRegras.validar("   ", 5))

    @Test fun tituloCom61_eLongo() = assertTrue(ErroQuest.TITULO_LONGO in QuestRegras.validar("a".repeat(61), 5))

    @Test fun tituloCom60_eValido() = assertTrue(QuestRegras.validar("a".repeat(60), 5).isEmpty())

    @Test fun rodadas0e21_foraDoIntervalo() {
        listOf(0, 21).forEach { assertTrue(ErroQuest.RODADAS_FORA_DO_INTERVALO in QuestRegras.validar("Física", it)) }
    }

    @Test fun rodadasAbaixoDoConcluido() =
        assertTrue(ErroQuest.RODADAS_ABAIXO_DO_CONCLUIDO in QuestRegras.validar("Física", 2, rodadasConcluidas = 3))

    @Test fun previa5Rodadas() = assertEquals("5 batalhas = 125 min de foco + 20 min de pousada", QuestRegras.textoPrevia(5, 25, 5))

    @Test fun previa1Rodada() = assertEquals("1 batalha = 25 min de foco", QuestRegras.textoPrevia(1, 25, 5))

    @Test fun pendentes_excluiConcluidas() =
        assertEquals(listOf(1L, 2L), QuestRegras.filtrarEOrdenar(todas, Filtro.PENDENTES, Ordem.PRIORIDADE).map { it.id })

    @Test fun ordemPrioridade_altaPrimeiro_empateMaisRecentePrimeiro() {
        val altaNova = altaPendenteAntiga.copy(id = 4, dataCriacao = 500)
        assertEquals(listOf(4L, 1L, 2L), QuestRegras.filtrarEOrdenar(listOf(baixaPendenteNova, altaPendenteAntiga, altaNova), Filtro.PENDENTES, Ordem.PRIORIDADE).map { it.id })
    }

    @Test fun ordemData_maisRecentePrimeiro() =
        assertEquals(listOf(2L, 1L), QuestRegras.filtrarEOrdenar(todas, Filtro.PENDENTES, Ordem.DATA).map { it.id })

    @Test fun todas_pendentesAntesDasConcluidas() =
        assertEquals(listOf(1L, 2L, 3L), QuestRegras.filtrarEOrdenar(todas, Filtro.TODAS, Ordem.PRIORIDADE).map { it.id })

    @Test fun concluidas_soConcluidas() =
        assertEquals(listOf(3L), QuestRegras.filtrarEOrdenar(todas, Filtro.CONCLUIDAS, Ordem.DATA).map { it.id })
}
