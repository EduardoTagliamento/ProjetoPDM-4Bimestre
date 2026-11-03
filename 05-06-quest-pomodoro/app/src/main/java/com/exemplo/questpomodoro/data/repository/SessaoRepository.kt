package com.exemplo.questpomodoro.data.repository

import com.exemplo.questpomodoro.data.local.BatalhaDao
import com.exemplo.questpomodoro.data.local.QuestDao
import com.exemplo.questpomodoro.data.local.SessaoDao
import com.exemplo.questpomodoro.data.prefs.ConfiguracoesStore
import com.exemplo.questpomodoro.data.prefs.HeroiStore
import com.exemplo.questpomodoro.domain.Heroi
import com.exemplo.questpomodoro.domain.Relogio
import com.exemplo.questpomodoro.domain.SessaoMaquina
import com.exemplo.questpomodoro.domain.model.BatalhaRegistro
import com.exemplo.questpomodoro.domain.model.EventoSessao
import com.exemplo.questpomodoro.domain.model.Fase
import com.exemplo.questpomodoro.domain.model.Sessao
import com.exemplo.questpomodoro.domain.model.Vitoria
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Orquestra a sessão ativa. Toda operação roda sob um único [Mutex] e começa
 * resolvendo fases expiradas, então chamadas concorrentes (alarme + UI) aplicam
 * cada fim de fase exatamente uma vez.
 */
class SessaoRepository(
    private val questDao: QuestDao,
    private val batalhaDao: BatalhaDao,
    private val sessaoDao: SessaoDao,
    private val heroi: HeroiStore,
    private val config: ConfiguracoesStore,
    private val transacao: Transacionador,
    private val relogio: Relogio,
    private val efeitos: SessaoEfeitos,
) {
    private val mutex = Mutex()

    val sessao: Flow<Sessao?> = sessaoDao.observar()

    suspend fun iniciar(questId: Long): Boolean = mutex.withLock {
        if (avancarInterno() != null) return false
        val quest = questDao.buscar(questId) ?: return false
        if (quest.concluida || quest.rodadasConcluidas >= quest.rodadasTotais) return false
        val c = config.atual()
        val nova = SessaoMaquina.iniciar(quest, c.focoMin, c.descansoMin, relogio.agora())
        sessaoDao.salvar(nova)
        efeitos.aoMudarSessao(nova)
        true
    }

    suspend fun pausar() = operar { s, agora -> SessaoMaquina.pausar(s, agora) }

    suspend fun retomar() = operar { s, agora -> SessaoMaquina.retomar(s, agora) }

    suspend fun pularDescanso() = operar { s, _ -> SessaoMaquina.pularDescanso(s) }

    suspend fun iniciarProxima() = operar { s, agora -> SessaoMaquina.iniciarProxima(s, agora) }

    suspend fun desistir() = mutex.withLock {
        val atual = avancarInterno() ?: return
        if (atual.fase == Fase.FOCO || atual.fase == Fase.PAUSADO) {
            batalhaDao.inserir(
                BatalhaRegistro(questId = atual.questId, inicio = atual.faseIniciadaEm, fim = relogio.agora(), concluida = false),
            )
        }
        sessaoDao.limpar()
        efeitos.aoMudarSessao(null)
    }

    suspend fun sincronizar() {
        mutex.withLock { avancarInterno() }
    }

    private suspend fun operar(op: (Sessao, Long) -> Sessao) = mutex.withLock {
        val atual = avancarInterno() ?: return
        val nova = op(atual, relogio.agora())
        if (nova != atual) {
            sessaoDao.salvar(nova)
            efeitos.aoMudarSessao(nova)
        }
    }

    /** Aplica fases expiradas e devolve a sessão resultante. Chamar só com o mutex. */
    private suspend fun avancarInterno(): Sessao? {
        val atual = sessaoDao.buscar() ?: return null
        val transicao = SessaoMaquina.avancar(atual, relogio.agora())
        if (transicao.eventos.isEmpty()) return atual

        val titulo = questDao.buscar(atual.questId)?.titulo ?: ""
        transacao.rodar {
            val xpAntes = heroi.xpAtual()
            for (evento in transicao.eventos) {
                when (evento) {
                    is EventoSessao.BatalhaConcluida -> {
                        questDao.buscar(evento.questId)?.let { questDao.atualizar(it.copy(rodadasConcluidas = evento.rodada)) }
                        batalhaDao.inserir(
                            BatalhaRegistro(questId = evento.questId, inicio = evento.inicio, fim = evento.fim, concluida = true),
                        )
                        heroi.adicionarXp(Heroi.XP_POR_BATALHA)
                    }
                    is EventoSessao.QuestConcluida -> {
                        questDao.buscar(evento.questId)?.let {
                            questDao.atualizar(it.copy(concluida = true, dataConclusao = evento.fim))
                        }
                        heroi.adicionarXp(Heroi.BONUS_QUEST)
                        heroi.registrarVitoria(
                            Vitoria(
                                questId = evento.questId,
                                titulo = titulo,
                                xpGanho = atual.rodadasTotais * Heroi.XP_POR_BATALHA + Heroi.BONUS_QUEST,
                                nivelAntes = Heroi.nivel(xpAntes),
                                nivelDepois = Heroi.nivel(heroi.xpAtual()),
                            ),
                        )
                    }
                    is EventoSessao.DescansoTerminou -> Unit
                }
            }
            val nova = transicao.sessao
            if (nova == null) sessaoDao.limpar() else sessaoDao.salvar(nova)
        }
        efeitos.aoMudarSessao(transicao.sessao)
        efeitos.alertar(alertaPara(transicao.eventos.last(), titulo, atual.rodadasTotais))
        return transicao.sessao
    }

    private fun alertaPara(evento: EventoSessao, titulo: String, total: Int): Alerta = when (evento) {
        is EventoSessao.QuestConcluida -> Alerta.BatalhaFinal(titulo)
        is EventoSessao.BatalhaConcluida -> Alerta.FimDoFoco(titulo, evento.rodada + 1, total)
        is EventoSessao.DescansoTerminou -> Alerta.FimDoDescanso(titulo, evento.proximaRodada, total)
    }
}
