package com.exemplo.questpomodoro.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface ConfiguracoesStore {
    val configuracoes: Flow<Configuracoes>
    suspend fun atual(): Configuracoes
    suspend fun atualizar(t: (Configuracoes) -> Configuracoes)
}

class DataStoreConfiguracoes(private val context: Context) : ConfiguracoesStore {
    private object Chaves {
        val FOCO = intPreferencesKey("foco_min")
        val DESCANSO = intPreferencesKey("descanso_min")
        val APPS = stringSetPreferencesKey("apps_liberados")
        val LEMBRETE_ATIVO = booleanPreferencesKey("lembrete_ativo")
        val LEMBRETE_HORA = intPreferencesKey("lembrete_hora")
        val LEMBRETE_MINUTO = intPreferencesKey("lembrete_minuto")
    }

    private fun ler(p: Preferences): Configuracoes {
        val padrao = Configuracoes()
        return Configuracoes(
            focoMin = p[Chaves.FOCO] ?: padrao.focoMin,
            descansoMin = p[Chaves.DESCANSO] ?: padrao.descansoMin,
            appsLiberados = p[Chaves.APPS] ?: padrao.appsLiberados,
            lembreteAtivo = p[Chaves.LEMBRETE_ATIVO] ?: padrao.lembreteAtivo,
            lembreteHora = p[Chaves.LEMBRETE_HORA] ?: padrao.lembreteHora,
            lembreteMinuto = p[Chaves.LEMBRETE_MINUTO] ?: padrao.lembreteMinuto,
        ).normalizada()
    }

    override val configuracoes: Flow<Configuracoes> = context.questDataStore.data.map(::ler)

    override suspend fun atual(): Configuracoes = configuracoes.first()

    override suspend fun atualizar(t: (Configuracoes) -> Configuracoes) {
        context.questDataStore.edit { p ->
            val nova = t(ler(p)).normalizada()
            p[Chaves.FOCO] = nova.focoMin
            p[Chaves.DESCANSO] = nova.descansoMin
            p[Chaves.APPS] = nova.appsLiberados
            p[Chaves.LEMBRETE_ATIVO] = nova.lembreteAtivo
            p[Chaves.LEMBRETE_HORA] = nova.lembreteHora
            p[Chaves.LEMBRETE_MINUTO] = nova.lembreteMinuto
        }
    }
}
