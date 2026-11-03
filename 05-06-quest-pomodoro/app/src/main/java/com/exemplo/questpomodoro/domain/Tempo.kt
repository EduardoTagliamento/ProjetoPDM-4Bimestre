package com.exemplo.questpomodoro.domain

fun interface Relogio {
    fun agora(): Long
}

/** "mm:ss", arredondando os segundos para cima. */
fun formatarTempo(ms: Long): String {
    val totalSeg = (ms.coerceAtLeast(0L) + 999) / 1000
    return "%02d:%02d".format(totalSeg / 60, totalSeg % 60)
}
