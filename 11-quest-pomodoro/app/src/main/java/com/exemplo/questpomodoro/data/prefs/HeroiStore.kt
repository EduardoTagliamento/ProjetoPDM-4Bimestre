package com.exemplo.questpomodoro.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.exemplo.questpomodoro.domain.model.Vitoria
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Arquivo único de preferências do app (configurações + herói). */
val Context.questDataStore: DataStore<Preferences> by preferencesDataStore(name = "quest_prefs")

interface HeroiStore {
    val xp: Flow<Int>
    suspend fun xpAtual(): Int
    suspend fun adicionarXp(qtd: Int)
    val vitoriaPendente: Flow<Vitoria?>
    suspend fun registrarVitoria(v: Vitoria)
    suspend fun limparVitoria()
}

class DataStoreHeroi(private val context: Context) : HeroiStore {
    private object Chaves {
        val XP = intPreferencesKey("xp")
        val V_QUEST = longPreferencesKey("vitoria_quest_id")
        val V_TITULO = stringPreferencesKey("vitoria_titulo")
        val V_XP = intPreferencesKey("vitoria_xp")
        val V_ANTES = intPreferencesKey("vitoria_nivel_antes")
        val V_DEPOIS = intPreferencesKey("vitoria_nivel_depois")
    }

    override val xp: Flow<Int> = context.questDataStore.data.map { it[Chaves.XP] ?: 0 }

    override suspend fun xpAtual(): Int = xp.first()

    override suspend fun adicionarXp(qtd: Int) {
        context.questDataStore.edit { it[Chaves.XP] = (it[Chaves.XP] ?: 0) + qtd }
    }

    override val vitoriaPendente: Flow<Vitoria?> = context.questDataStore.data.map { p ->
        val questId = p[Chaves.V_QUEST] ?: return@map null
        Vitoria(
            questId = questId,
            titulo = p[Chaves.V_TITULO] ?: "",
            xpGanho = p[Chaves.V_XP] ?: 0,
            nivelAntes = p[Chaves.V_ANTES] ?: 1,
            nivelDepois = p[Chaves.V_DEPOIS] ?: 1,
        )
    }

    override suspend fun registrarVitoria(v: Vitoria) {
        context.questDataStore.edit {
            it[Chaves.V_QUEST] = v.questId
            it[Chaves.V_TITULO] = v.titulo
            it[Chaves.V_XP] = v.xpGanho
            it[Chaves.V_ANTES] = v.nivelAntes
            it[Chaves.V_DEPOIS] = v.nivelDepois
        }
    }

    override suspend fun limparVitoria() {
        context.questDataStore.edit {
            it.remove(Chaves.V_QUEST)
            it.remove(Chaves.V_TITULO)
            it.remove(Chaves.V_XP)
            it.remove(Chaves.V_ANTES)
            it.remove(Chaves.V_DEPOIS)
        }
    }
}
