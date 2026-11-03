package com.exemplo.questpomodoro.data.repository

import com.exemplo.questpomodoro.data.local.QuestDao
import com.exemplo.questpomodoro.data.local.SessaoDao
import com.exemplo.questpomodoro.domain.ErroQuest
import com.exemplo.questpomodoro.domain.QuestRegras
import com.exemplo.questpomodoro.domain.Relogio
import com.exemplo.questpomodoro.domain.model.Quest
import kotlinx.coroutines.flow.Flow

sealed interface ResultadoSalvar {
    data class Sucesso(val id: Long) : ResultadoSalvar
    data class Invalido(val erros: Set<ErroQuest>) : ResultadoSalvar
    data object Bloqueada : ResultadoSalvar
}

class QuestRepository(
    private val questDao: QuestDao,
    private val sessaoDao: SessaoDao,
    private val relogio: Relogio,
) {
    fun observarQuests(): Flow<List<Quest>> = questDao.observarTodas()

    fun observarQuest(id: Long): Flow<Quest?> = questDao.observar(id)

    /** [id] = 0 cria uma quest nova. */
    suspend fun salvar(id: Long, titulo: String, descricao: String, prioridade: Int, rodadasTotais: Int): ResultadoSalvar {
        if (id != 0L && emSessao(id)) return ResultadoSalvar.Bloqueada
        val existente = if (id == 0L) null else (questDao.buscar(id) ?: return ResultadoSalvar.Bloqueada)
        if (existente?.concluida == true) return ResultadoSalvar.Bloqueada

        val erros = QuestRegras.validar(titulo, rodadasTotais, existente?.rodadasConcluidas ?: 0)
        if (erros.isNotEmpty()) return ResultadoSalvar.Invalido(erros)

        val limpo = titulo.trim()
        return if (existente == null) {
            val novoId = questDao.inserir(
                Quest(
                    titulo = limpo,
                    descricao = descricao.trim(),
                    prioridade = prioridade,
                    rodadasTotais = rodadasTotais,
                    dataCriacao = relogio.agora(),
                ),
            )
            ResultadoSalvar.Sucesso(novoId)
        } else {
            questDao.atualizar(
                existente.copy(titulo = limpo, descricao = descricao.trim(), prioridade = prioridade, rodadasTotais = rodadasTotais),
            )
            ResultadoSalvar.Sucesso(existente.id)
        }
    }

    suspend fun excluir(id: Long): Boolean {
        if (emSessao(id)) return false
        questDao.excluir(id)
        return true
    }

    suspend fun contarPendentes(): Int = questDao.contarPendentes()

    private suspend fun emSessao(id: Long): Boolean = sessaoDao.buscar()?.questId == id
}
