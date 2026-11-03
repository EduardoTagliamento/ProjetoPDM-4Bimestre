package com.exemplo.questpomodoro.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.exemplo.questpomodoro.domain.model.Sessao
import kotlinx.coroutines.flow.Flow

@Dao
interface SessaoDao {
    @Query("SELECT * FROM sessao_ativa WHERE id = 0")
    fun observar(): Flow<Sessao?>

    @Query("SELECT * FROM sessao_ativa WHERE id = 0")
    suspend fun buscar(): Sessao?

    @Upsert
    suspend fun salvar(s: Sessao)

    @Query("DELETE FROM sessao_ativa")
    suspend fun limpar()
}
