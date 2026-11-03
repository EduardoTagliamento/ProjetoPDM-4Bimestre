package com.exemplo.questpomodoro.fakes

import com.exemplo.questpomodoro.data.repository.QuestRepository
import com.exemplo.questpomodoro.data.repository.SessaoRepository

/** Repositórios reais montados sobre fakes, para testes de ViewModel. */
class ReposDeTeste(xpInicial: Int = 0) {
    val questDao = FakeQuestDao()
    val batalhaDao = FakeBatalhaDao()
    val sessaoDao = FakeSessaoDao()
    val heroi = FakeHeroiStore(xpInicial)
    val config = FakeConfiguracoesStore()
    val relogio = FakeRelogio(0)
    val efeitos = FakeEfeitos()
    val questRepo = QuestRepository(questDao, sessaoDao, relogio)
    val sessaoRepo = SessaoRepository(questDao, batalhaDao, sessaoDao, heroi, config, TransacionadorDireto, relogio, efeitos)
}
