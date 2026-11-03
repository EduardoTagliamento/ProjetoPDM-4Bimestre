package com.exemplo.questpomodoro

import android.content.Context
import androidx.room.withTransaction
import com.exemplo.questpomodoro.data.local.AppDatabase
import com.exemplo.questpomodoro.data.prefs.ConfiguracoesStore
import com.exemplo.questpomodoro.data.prefs.DataStoreConfiguracoes
import com.exemplo.questpomodoro.data.prefs.DataStoreHeroi
import com.exemplo.questpomodoro.data.prefs.HeroiStore
import com.exemplo.questpomodoro.data.repository.QuestRepository
import com.exemplo.questpomodoro.data.repository.SessaoRepository
import com.exemplo.questpomodoro.data.repository.Transacionador
import com.exemplo.questpomodoro.domain.Relogio
import com.exemplo.questpomodoro.service.SessaoEfeitosAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Injeção de dependência manual. */
class AppContainer(context: Context) {
    private val app = context.applicationContext

    val db: AppDatabase = AppDatabase.criar(app)
    val config: ConfiguracoesStore = DataStoreConfiguracoes(app)
    val heroi: HeroiStore = DataStoreHeroi(app)
    val relogio: Relogio = Relogio { System.currentTimeMillis() }
    val escopo: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val efeitos = SessaoEfeitosAndroid(app)

    private val transacao = object : Transacionador {
        override suspend fun <R> rodar(bloco: suspend () -> R): R = db.withTransaction { bloco() }
    }

    val questRepository = QuestRepository(db.questDao(), db.sessaoDao(), relogio)
    val sessaoRepository = SessaoRepository(
        db.questDao(), db.batalhaDao(), db.sessaoDao(), heroi, config, transacao, relogio, efeitos,
    )
}

val Context.container: AppContainer
    get() = (applicationContext as QuestApp).container
