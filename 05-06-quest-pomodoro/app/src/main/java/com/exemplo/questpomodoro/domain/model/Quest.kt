package com.exemplo.questpomodoro.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quests")
data class Quest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val descricao: String = "",
    val prioridade: Int = Prioridade.MEDIA.valor,
    val rodadasTotais: Int,
    val rodadasConcluidas: Int = 0,
    val concluida: Boolean = false,
    val dataCriacao: Long,
    val dataConclusao: Long? = null,
)
