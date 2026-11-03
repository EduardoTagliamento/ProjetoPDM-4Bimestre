package com.exemplo.questpomodoro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.exemplo.questpomodoro.domain.model.Quest
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    @Query("SELECT * FROM quests")
    fun observarTodas(): Flow<List<Quest>>

    @Query("SELECT * FROM quests WHERE id = :id")
    fun observar(id: Long): Flow<Quest?>

    @Query("SELECT * FROM quests WHERE id = :id")
    suspend fun buscar(id: Long): Quest?

    @Insert
    suspend fun inserir(q: Quest): Long

    @Update
    suspend fun atualizar(q: Quest)

    @Query("DELETE FROM quests WHERE id = :id")
    suspend fun excluir(id: Long)

    @Query("SELECT COUNT(*) FROM quests WHERE concluida = 0")
    suspend fun contarPendentes(): Int
}
