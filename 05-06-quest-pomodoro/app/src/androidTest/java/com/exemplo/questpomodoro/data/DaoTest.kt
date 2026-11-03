package com.exemplo.questpomodoro.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.exemplo.questpomodoro.data.local.AppDatabase
import com.exemplo.questpomodoro.domain.model.Fase
import com.exemplo.questpomodoro.domain.model.Quest
import com.exemplo.questpomodoro.domain.model.Sessao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun abrir() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
        ).build()
    }

    @After
    fun fechar() = db.close()

    private fun sessao(rodada: Int) = Sessao(
        questId = 1, rodadasTotais = 5, fase = Fase.FOCO, rodadaAtual = rodada,
        faseIniciadaEm = 0, faseTerminaEm = 10, restanteAoPausarMs = null, focoMs = 10, descansoMs = 5,
    )

    @Test
    fun inserirEBuscarQuest() = runTest {
        val id = db.questDao().inserir(Quest(titulo = "Física", rodadasTotais = 5, dataCriacao = 1))
        assertEquals("Física", db.questDao().buscar(id)?.titulo)
    }

    @Test
    fun sessaoTemLinhaUnica() = runTest {
        db.sessaoDao().salvar(sessao(1))
        db.sessaoDao().salvar(sessao(2))
        assertEquals(2, db.sessaoDao().buscar()?.rodadaAtual)
        db.sessaoDao().limpar()
        assertNull(db.sessaoDao().buscar())
    }

    @Test
    fun contarPendentesIgnoraConcluidas() = runTest {
        db.questDao().inserir(Quest(titulo = "A", rodadasTotais = 1, dataCriacao = 1))
        db.questDao().inserir(Quest(titulo = "B", rodadasTotais = 1, rodadasConcluidas = 1, concluida = true, dataCriacao = 1))
        assertEquals(1, db.questDao().contarPendentes())
    }
}
