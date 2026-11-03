package com.exemplo.questpomodoro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.exemplo.questpomodoro.domain.model.BatalhaRegistro
import com.exemplo.questpomodoro.domain.model.Quest
import com.exemplo.questpomodoro.domain.model.Sessao

@Database(entities = [Quest::class, BatalhaRegistro::class, Sessao::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questDao(): QuestDao
    abstract fun batalhaDao(): BatalhaDao
    abstract fun sessaoDao(): SessaoDao

    companion object {
        fun criar(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "quest.db").build()
    }
}
