package com.exemplo.questpomodoro.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "batalhas")
data class BatalhaRegistro(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questId: Long,
    val inicio: Long,
    val fim: Long,
    val concluida: Boolean,
)
