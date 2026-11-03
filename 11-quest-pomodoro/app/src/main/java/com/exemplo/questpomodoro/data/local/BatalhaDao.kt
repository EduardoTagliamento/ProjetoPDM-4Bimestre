package com.exemplo.questpomodoro.data.local

import androidx.room.Dao
import androidx.room.Insert
import com.exemplo.questpomodoro.domain.model.BatalhaRegistro

@Dao
interface BatalhaDao {
    @Insert
    suspend fun inserir(b: BatalhaRegistro)
}
