package com.exemplo.questpomodoro.data.prefs

data class Configuracoes(
    val focoMin: Int = 25,
    val descansoMin: Int = 5,
    val appsLiberados: Set<String> = emptySet(),
    val lembreteAtivo: Boolean = true,
    val lembreteHora: Int = 19,
    val lembreteMinuto: Int = 0,
) {
    fun normalizada(): Configuracoes = copy(
        focoMin = focoMin.coerceIn(FOCO_FAIXA),
        descansoMin = descansoMin.coerceIn(DESCANSO_FAIXA),
        lembreteHora = lembreteHora.coerceIn(0, 23),
        lembreteMinuto = lembreteMinuto.coerceIn(0, 59),
    )

    companion object {
        val FOCO_FAIXA = 1..60
        val DESCANSO_FAIXA = 1..30
    }
}
