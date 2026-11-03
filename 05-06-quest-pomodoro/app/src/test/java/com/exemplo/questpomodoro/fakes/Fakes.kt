package com.exemplo.questpomodoro.fakes

import com.exemplo.questpomodoro.data.local.BatalhaDao
import com.exemplo.questpomodoro.data.local.QuestDao
import com.exemplo.questpomodoro.data.local.SessaoDao
import com.exemplo.questpomodoro.data.prefs.Configuracoes
import com.exemplo.questpomodoro.data.prefs.ConfiguracoesStore
import com.exemplo.questpomodoro.data.prefs.HeroiStore
import com.exemplo.questpomodoro.data.repository.Alerta
import com.exemplo.questpomodoro.data.repository.SessaoEfeitos
import com.exemplo.questpomodoro.data.repository.Transacionador
import com.exemplo.questpomodoro.domain.Relogio
import com.exemplo.questpomodoro.domain.model.BatalhaRegistro
import com.exemplo.questpomodoro.domain.model.Quest
import com.exemplo.questpomodoro.domain.model.Sessao
import com.exemplo.questpomodoro.domain.model.Vitoria
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.yield

class FakeQuestDao : QuestDao {
    val quests = MutableStateFlow<List<Quest>>(emptyList())
    private var proximoId = 1L

    override fun observarTodas(): Flow<List<Quest>> = quests
    override fun observar(id: Long): Flow<Quest?> = quests.map { l -> l.firstOrNull { it.id == id } }
    override suspend fun buscar(id: Long): Quest? {
        yield() // simula suspensão real, para expor corridas
        return quests.value.firstOrNull { it.id == id }
    }
    override suspend fun inserir(q: Quest): Long {
        val id = if (q.id == 0L) proximoId++ else q.id
        quests.value = quests.value + q.copy(id = id)
        return id
    }
    override suspend fun atualizar(q: Quest) {
        quests.value = quests.value.map { if (it.id == q.id) q else it }
    }
    override suspend fun excluir(id: Long) {
        quests.value = quests.value.filterNot { it.id == id }
    }
    override suspend fun contarPendentes(): Int = quests.value.count { !it.concluida }
}

class FakeBatalhaDao : BatalhaDao {
    val registros = mutableListOf<BatalhaRegistro>()
    override suspend fun inserir(b: BatalhaRegistro) {
        registros += b
    }
}

class FakeSessaoDao : SessaoDao {
    val sessao = MutableStateFlow<Sessao?>(null)
    override fun observar(): Flow<Sessao?> = sessao
    override suspend fun buscar(): Sessao? {
        yield()
        return sessao.value
    }
    override suspend fun salvar(s: Sessao) {
        sessao.value = s
    }
    override suspend fun limpar() {
        sessao.value = null
    }
}

class FakeHeroiStore(xpInicial: Int = 0) : HeroiStore {
    override val xp = MutableStateFlow(xpInicial)
    override val vitoriaPendente = MutableStateFlow<Vitoria?>(null)
    override suspend fun xpAtual(): Int = xp.value
    override suspend fun adicionarXp(qtd: Int) {
        xp.value += qtd
    }
    override suspend fun registrarVitoria(v: Vitoria) {
        vitoriaPendente.value = v
    }
    override suspend fun limparVitoria() {
        vitoriaPendente.value = null
    }
}

class FakeConfiguracoesStore(inicial: Configuracoes = Configuracoes()) : ConfiguracoesStore {
    val estado = MutableStateFlow(inicial)
    var c: Configuracoes
        get() = estado.value
        set(v) { estado.value = v }
    override val configuracoes: Flow<Configuracoes> = estado
    override suspend fun atual(): Configuracoes = estado.value
    override suspend fun atualizar(t: (Configuracoes) -> Configuracoes) {
        estado.value = t(estado.value).normalizada()
    }
}

class FakeRelogio(var t: Long = 0L) : Relogio {
    override fun agora(): Long = t
}

class FakeEfeitos : SessaoEfeitos {
    val sessoes = mutableListOf<Sessao?>()
    val alertas = mutableListOf<Alerta>()
    override fun aoMudarSessao(sessao: Sessao?) {
        sessoes += sessao
    }
    override fun alertar(alerta: Alerta) {
        alertas += alerta
    }
}

object TransacionadorDireto : Transacionador {
    override suspend fun <R> rodar(bloco: suspend () -> R): R = bloco()
}
