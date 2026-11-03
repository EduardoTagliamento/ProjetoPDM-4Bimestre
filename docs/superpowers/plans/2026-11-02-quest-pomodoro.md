# Quest Pomodoro Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** App Android (Kotlin + Compose) que junta lista de tarefas e pomodoro: cada Quest tem N rodadas de foco, os outros apps são bloqueados durante o foco, há notificações e o tema é RPG pixel art.

**Architecture:** App de um módulo, MVVM. Toda regra de negócio fica em funções puras em `domain/` (máquina de estados da sessão, XP, validação, bloqueio, lembrete), testadas na JVM. Os repositórios orquestram Room/DataStore através de interfaces (DAOs, stores, `SessaoEfeitos`), para serem testados com fakes. A camada Android (foreground service, AlarmManager, AccessibilityService, WorkManager e Compose) só observa e reage, sem decidir nada.

**Tech Stack:** Kotlin 2.2.20, AGP 8.13.0, Gradle 8.14.3, Compose BOM 2025.09.01 + Material 3, Navigation Compose 2.9.5, Lifecycle/ViewModel 2.9.4, Room 2.8.0 (KSP 2.2.20-2.0.3), DataStore Preferences 1.1.7, WorkManager 2.10.5, Coroutines 1.10.2, JUnit 4.13.2. Se alguma versão não resolver, use a estável mais recente compatível e registre-a em `gradle/libs.versions.toml`.

**Spec:** `docs/superpowers/specs/2026-11-02-quest-pomodoro-design.md` (leia antes de cada task).

## Global Constraints

- Projeto em `05-06-quest-pomodoro/` na raiz do repositório; pacote `com.exemplo.questpomodoro`.
- `minSdk 26`, `compileSdk 36`, `targetSdk 36` (a plataforma `android-36` está instalada).
- Todos os comandos Gradle rodam no Git Bash, de dentro de `05-06-quest-pomodoro/`, com `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` (JDK 21; o JDK 25 do PATH não serve).
- `local.properties` com `sdk.dir=C\:\\Users\\eduar\\AppData\\Local\\Android\\Sdk`, fora do git.
- Branch de trabalho: `feat/quest-pomodoro`. Nunca commitar direto na `main`.
- Textos visíveis em **pt-BR**, em `res/values/strings.xml`, salvo strings montadas por funções de domínio testadas (copy fixa abaixo).
- Padrões: foco **25 min** (faixa 1–60), descanso **5 min** (faixa 1–30), rodadas **1–20**, título **até 60** caracteres, lembrete **19:00** ligado.
- XP: **+10** por batalha concluída, **+50** ao concluir a quest, nível = `1 + xp / 100`. Desistir não tira XP.
- Sem descanso depois da última rodada. Ao fim do descanso, espera o usuário (AGUARDANDO). A pausa só existe no FOCO e libera os apps.
- O tempo restante é sempre derivado de timestamps (`faseTerminaEm - agora`), nunca de um contador decrementado.
- Toda arte é original (sprites desenhados em código) ou de licença livre: fonte Press Start 2P (OFL) e paleta Sweetie 16 (CC0). Nada de sprites ou sons de franquias.
- Injeção de dependência manual (`AppContainer` no `Application`), sem Hilt *(decisão do Claude)*.

## Review Focus

1. **Toque duplo em "Iniciar"** (botão da tela + ação da notificação): deve existir uma única sessão; a segunda chamada é ignorada. Teste na Task 5.
2. **Alarme e UI sincronizando ao mesmo tempo no fim da fase:** a rodada é contada uma vez só e o alerta toca uma vez só. Teste na Task 5.
3. **Ligação recebida durante o foco:** a tela de chamada (in-call UI) nunca pode ser bloqueada. Teste na Task 11.
4. **Mudar as durações nas Configurações durante uma sessão:** a sessão atual não muda; a próxima usa os valores novos. Teste na Task 5.
5. **Editar ou excluir a quest da sessão ativa** (o formulário aberto antes de começar a batalha): a operação é recusada. Teste na Task 5.

---

## File Structure

Caminhos relativos a `05-06-quest-pomodoro/`. `PKG` = `app/src/main/java/com/exemplo/questpomodoro`, `TST` = `app/src/test/java/com/exemplo/questpomodoro`.

| Arquivo | Responsabilidade |
|---|---|
| `PKG/QuestApp.kt`, `PKG/AppContainer.kt`, `PKG/MainActivity.kt` | Application, DI manual, host do NavHost |
| `PKG/domain/model/{Quest,Prioridade,Sessao,BatalhaRegistro,Vitoria}.kt` | Modelos (as entidades Room ficam aqui mesmo) |
| `PKG/domain/QuestRegras.kt` | Validação, prévia, filtro e ordem |
| `PKG/domain/Heroi.kt` | XP e nível |
| `PKG/domain/SessaoMaquina.kt` | Máquina de estados pura da sessão |
| `PKG/domain/Tempo.kt` | `Relogio`, `formatarTempo` |
| `PKG/domain/RegrasBloqueio.kt` | `deveBloquear`, pacotes do sistema, `servicoAcessibilidadeAtivo` |
| `PKG/domain/Lembrete.kt` | `atrasoAteProximo`, `textoLembrete` |
| `PKG/data/local/{AppDatabase,QuestDao,BatalhaDao,SessaoDao}.kt` | Room |
| `PKG/data/prefs/{Configuracoes,ConfiguracoesStore,HeroiStore}.kt` | DataStore |
| `PKG/data/repository/{QuestRepository,SessaoRepository,SessaoEfeitos,Transacionador}.kt` | Orquestração |
| `PKG/notification/{Canais,ConteudoNotificacao,Notificador,LembreteWorker,LembreteAgendador}.kt` | Notificações |
| `PKG/service/{SessaoService,AgendadorAlarme,SessaoEfeitosAndroid,FaseAlarmReceiver,SessaoAcaoReceiver,BootReceiver,BloqueioAccessibilityService,SempreLiberados}.kt` | Componentes Android |
| `PKG/ui/theme/{Cores,Tipografia,Tema}.kt` | Tema RPG |
| `PKG/ui/components/{PixelPanel,PixelButton,SpritePixel,Sprites,TimerCircular,BarraXp,ProgressoRodadas}.kt` | Componentes pixel |
| `PKG/ui/{Navegacao.kt, taverna/, quest/, batalha/, config/, bloqueio/}` | Telas + ViewModels |

---

### Task 1: Esqueleto do projeto que compila

**Files:**
- Create: `05-06-quest-pomodoro/{settings.gradle.kts,build.gradle.kts,gradle.properties,gradle/libs.versions.toml,.gitignore,local.properties}`
- Create: `05-06-quest-pomodoro/app/{build.gradle.kts,proguard-rules.pro}`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/{strings.xml,themes.xml}`
- Create: `PKG/QuestApp.kt`, `PKG/MainActivity.kt`
- Test: `TST/SanidadeTest.kt`

**Interfaces:**
- Produces: módulo `:app` com plugins `com.android.application`, `org.jetbrains.kotlin.android`, `org.jetbrains.kotlin.plugin.compose` e `com.google.devtools.ksp`; as dependências de todas as tasks já declaradas no catálogo (compose-bom, material3, material-icons-extended, activity-compose, navigation-compose, lifecycle-viewmodel-compose, lifecycle-runtime-compose, room-runtime/ktx/compiler(ksp), datastore-preferences, work-runtime-ktx, core-ktx, coroutines-android; testes: junit, kotlinx-coroutines-test).
- Produces: `class QuestApp : Application()`, registrada no manifest; `MainActivity : ComponentActivity` mostrando `Text("Taverna")`.

- [ ] **Step 1: Criar a branch**

```bash
cd "/f/Area de Trabalho/bruno/ProjetoPDM-4Bimestre" && git checkout -b feat/quest-pomodoro
```

- [ ] **Step 2: Criar os arquivos Gradle e o manifest**, com as versões do Tech Stack. `app/build.gradle.kts`: `namespace = "com.exemplo.questpomodoro"`, `applicationId` igual, `buildFeatures { compose = true }`, Java/Kotlin target 17, `ksp { arg("room.generateKotlin", "true") }`. Em `themes.xml`, um tema `Theme.QuestPomodoro` baseado em `android:Theme.Material.NoActionBar` com `windowBackground` `#1A1C2C`. O `.gitignore` cobre `build/`, `.gradle/`, `local.properties`, `.idea/` e `*.iml`.

- [ ] **Step 3: Gerar o wrapper com o Gradle em cache**

```bash
cd 05-06-quest-pomodoro && export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
"$(ls -d ~/.gradle/wrapper/dists/gradle-8.14.3-bin/*/gradle-8.14.3)/bin/gradle" wrapper --gradle-version 8.14.3
```
Expected: `BUILD SUCCESSFUL`, com `gradlew` e `gradle/wrapper/` criados.

- [ ] **Step 4: Escrever `SanidadeTest.somaFunciona`** (`assertEquals(4, 2 + 2)`), só para validar o pipeline de testes.

- [ ] **Step 5: Compilar e rodar os testes**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add 05-06-quest-pomodoro && git commit -m "chore(quest-pomodoro): esqueleto do projeto Android"
```

---

### Task 2: Regras puras de Quest e Herói

**Files:**
- Create: `PKG/domain/model/{Quest,Prioridade}.kt`, `PKG/domain/QuestRegras.kt`, `PKG/domain/Heroi.kt`
- Test: `TST/domain/QuestRegrasTest.kt`, `TST/domain/HeroiTest.kt`

**Interfaces:**
- Produces:
```kotlin
@Entity(tableName = "quests")
data class Quest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String, val descricao: String = "",
    val prioridade: Int = Prioridade.MEDIA.valor,
    val rodadasTotais: Int, val rodadasConcluidas: Int = 0,
    val concluida: Boolean = false, val dataCriacao: Long, val dataConclusao: Long? = null,
)
enum class Prioridade(val valor: Int, val raridade: String) {
    BAIXA(1, "Comum"), MEDIA(2, "Rara"), ALTA(3, "Épica");
    companion object { fun de(valor: Int): Prioridade }   // valor desconhecido → MEDIA
}
enum class ErroQuest { TITULO_VAZIO, TITULO_LONGO, RODADAS_FORA_DO_INTERVALO, RODADAS_ABAIXO_DO_CONCLUIDO }
enum class Filtro { TODAS, PENDENTES, CONCLUIDAS }
enum class Ordem { PRIORIDADE, DATA }
object QuestRegras {
    const val TITULO_MAX = 60
    val RODADAS = 1..20
    fun validar(titulo: String, rodadasTotais: Int, rodadasConcluidas: Int = 0): Set<ErroQuest>
    fun textoPrevia(rodadas: Int, focoMin: Int, descansoMin: Int): String
    fun filtrarEOrdenar(quests: List<Quest>, filtro: Filtro, ordem: Ordem): List<Quest>
}
object Heroi {
    const val XP_POR_BATALHA = 10; const val BONUS_QUEST = 50; const val XP_POR_NIVEL = 100
    fun nivel(xp: Int): Int          // 1 + xp / 100
    fun xpNoNivel(xp: Int): Int      // xp % 100
}
```

- [ ] **Step 1: Escrever os testes que falham**

```kotlin
// QuestRegrasTest
@Test fun tituloSoComEspacos_eVazio() = assertEquals(setOf(ErroQuest.TITULO_VAZIO), QuestRegras.validar("   ", 5))
@Test fun tituloCom61_eLongo() = assertTrue(ErroQuest.TITULO_LONGO in QuestRegras.validar("a".repeat(61), 5))
@Test fun tituloCom60_eValido() = assertTrue(QuestRegras.validar("a".repeat(60), 5).isEmpty())
@Test fun rodadas0e21_foraDoIntervalo() { listOf(0, 21).forEach { assertTrue(ErroQuest.RODADAS_FORA_DO_INTERVALO in QuestRegras.validar("Física", it)) } }
@Test fun rodadasAbaixoDoConcluido() = assertTrue(ErroQuest.RODADAS_ABAIXO_DO_CONCLUIDO in QuestRegras.validar("Física", 2, rodadasConcluidas = 3))
@Test fun previa5Rodadas() = assertEquals("5 batalhas = 125 min de foco + 20 min de pousada", QuestRegras.textoPrevia(5, 25, 5))
@Test fun previa1Rodada() = assertEquals("1 batalha = 25 min de foco", QuestRegras.textoPrevia(1, 25, 5))
@Test fun pendentes_excluiConcluidas()
@Test fun ordemPrioridade_altaPrimeiro_empateMaisRecentePrimeiro()
@Test fun todas_pendentesAntesDasConcluidas()
// HeroiTest
@Test fun nivel() { assertEquals(1, Heroi.nivel(0)); assertEquals(1, Heroi.nivel(99)); assertEquals(2, Heroi.nivel(100)); assertEquals(3, Heroi.nivel(250)) }
@Test fun xpNoNivel() = assertEquals(50, Heroi.xpNoNivel(250))
```
Os três testes de filtro/ordem montam 3 quests (ALTA pendente antiga, BAIXA pendente nova, MEDIA concluída) e conferem a ordem dos `id`s.

- [ ] **Step 2: Rodar e ver falhar**: `./gradlew :app:testDebugUnitTest --tests "*QuestRegrasTest" --tests "*HeroiTest"`. Expected: falha de compilação (símbolos inexistentes).

- [ ] **Step 3: Implementar** as assinaturas acima. `validar` usa `titulo.trim()`. Em `filtrarEOrdenar`, `TODAS` põe as pendentes antes das concluídas; dentro de cada grupo, `PRIORIDADE` ordena por prioridade decrescente e depois por `dataCriacao` decrescente, e `DATA` ordena por `dataCriacao` decrescente.

- [ ] **Step 4: Rodar e ver passar** (mesmo comando). Expected: PASS.

- [ ] **Step 5: Commit**: `git commit -m "feat(quest-pomodoro): regras de quest, prévia e XP"`

---

### Task 3: Máquina de estados da sessão

**Files:**
- Create: `PKG/domain/model/Sessao.kt`, `PKG/domain/SessaoMaquina.kt`, `PKG/domain/Tempo.kt`
- Test: `TST/domain/SessaoMaquinaTest.kt`, `TST/domain/TempoTest.kt`

**Interfaces:**
- Consumes: `Quest` (Task 2).
- Produces:
```kotlin
enum class Fase { FOCO, PAUSADO, DESCANSO, AGUARDANDO }
@Entity(tableName = "sessao_ativa")
data class Sessao(
    @PrimaryKey val id: Int = 0,          // linha única → só uma sessão ativa
    val questId: Long, val rodadasTotais: Int, val fase: Fase,
    val rodadaAtual: Int,                 // batalha atual (FOCO/PAUSADO) ou próxima (DESCANSO/AGUARDANDO)
    val faseIniciadaEm: Long, val faseTerminaEm: Long?, val restanteAoPausarMs: Long?,
    val focoMs: Long, val descansoMs: Long,
)
sealed interface EventoSessao {
    data class BatalhaConcluida(val questId: Long, val rodada: Int, val inicio: Long, val fim: Long) : EventoSessao
    data class QuestConcluida(val questId: Long, val fim: Long) : EventoSessao
    data class DescansoTerminou(val questId: Long, val proximaRodada: Int) : EventoSessao
}
data class Transicao(val sessao: Sessao?, val eventos: List<EventoSessao>)
object SessaoMaquina {
    fun iniciar(quest: Quest, focoMin: Int, descansoMin: Int, agora: Long): Sessao
    fun pausar(s: Sessao, agora: Long): Sessao          // só FOCO; senão devolve s
    fun retomar(s: Sessao, agora: Long): Sessao         // só PAUSADO; senão devolve s
    fun pularDescanso(s: Sessao): Sessao                // só DESCANSO → AGUARDANDO
    fun iniciarProxima(s: Sessao, agora: Long): Sessao  // só AGUARDANDO → FOCO
    fun avancar(s: Sessao, agora: Long): Transicao      // resolve fases expiradas, em cadeia
    fun restanteMs(s: Sessao, agora: Long): Long        // ≥ 0; PAUSADO → restanteAoPausarMs; AGUARDANDO → 0
}
fun interface Relogio { fun agora(): Long }
fun formatarTempo(ms: Long): String   // "mm:ss", arredondando os segundos para cima; 0 → "00:00"
```

- [ ] **Step 1: Escrever os testes que falham.** Use `F = 25 * 60_000L`, `D = 5 * 60_000L` e `quest = Quest(id = 7, titulo = "Física", rodadasTotais = 5, rodadasConcluidas = 2, dataCriacao = 0)`.

```kotlin
@Test fun iniciar_comecaNaProximaRodada() { val s = SessaoMaquina.iniciar(quest, 25, 5, 1000)
    assertEquals(Fase.FOCO, s.fase); assertEquals(3, s.rodadaAtual); assertEquals(1000 + F, s.faseTerminaEm); assertEquals(5, s.rodadasTotais) }
@Test fun pausar_congelaRestante_eRetomar_reprojetaFim() {
    val p = SessaoMaquina.pausar(SessaoMaquina.iniciar(quest, 25, 5, 0), 60_000)
    assertEquals(Fase.PAUSADO, p.fase); assertEquals(F - 60_000, p.restanteAoPausarMs); assertNull(p.faseTerminaEm)
    assertEquals(F - 60_000, SessaoMaquina.restanteMs(p, 999_999_999))
    val r = SessaoMaquina.retomar(p, 500_000); assertEquals(500_000 + F - 60_000, r.faseTerminaEm) }
@Test fun pausar_noDescanso_naoFazNada()
@Test fun avancar_fimDoFoco_vaiParaDescanso_comRodadaSeguinte() {
    val t = SessaoMaquina.avancar(SessaoMaquina.iniciar(quest, 25, 5, 0), F)
    assertEquals(Fase.DESCANSO, t.sessao!!.fase); assertEquals(4, t.sessao!!.rodadaAtual); assertEquals(F + D, t.sessao!!.faseTerminaEm)
    assertEquals(listOf(EventoSessao.BatalhaConcluida(7, 3, 0, F)), t.eventos) }
@Test fun avancar_ultimaRodada_concluiQuest_semDescanso() {   // quest 5/5: rodadasConcluidas = 4
    val t = SessaoMaquina.avancar(SessaoMaquina.iniciar(quest.copy(rodadasConcluidas = 4), 25, 5, 0), F + 1)
    assertNull(t.sessao); assertEquals(listOf(EventoSessao.BatalhaConcluida(7, 5, 0, F), EventoSessao.QuestConcluida(7, F)), t.eventos) }
@Test fun avancar_focoEDescansoExpirados_emCadeia_paraEmAguardando() {   // processo morto
    val t = SessaoMaquina.avancar(SessaoMaquina.iniciar(quest, 25, 5, 0), F + D + 10)
    assertEquals(Fase.AGUARDANDO, t.sessao!!.fase); assertEquals(4, t.sessao!!.rodadaAtual)
    assertEquals(2, t.eventos.size); assertEquals(EventoSessao.DescansoTerminou(7, 4), t.eventos.last()) }
@Test fun avancar_semExpirar_semEventos()
@Test fun avancar_pausado_nuncaExpira()
@Test fun pularDescanso_vaiParaAguardando_eIniciarProxima_comecaFoco()
@Test fun restanteMs_nuncaNegativo()
// TempoTest
@Test fun formatar() { assertEquals("25:00", formatarTempo(F)); assertEquals("00:01", formatarTempo(1)); assertEquals("00:00", formatarTempo(0)); assertEquals("12:40", formatarTempo(760_000)) }
```

- [ ] **Step 2: Rodar e ver falhar**: `./gradlew :app:testDebugUnitTest --tests "*SessaoMaquinaTest" --tests "*TempoTest"`.

- [ ] **Step 3: Implementar.** `avancar` repete até não haver mais expiração: um FOCO expirado em `T = faseTerminaEm` emite `BatalhaConcluida(inicio = faseIniciadaEm, fim = T)`; se `rodadaAtual == rodadasTotais`, emite também `QuestConcluida(fim = T)` e devolve `sessao = null`; caso contrário vai para DESCANSO com `faseIniciadaEm = T`, `faseTerminaEm = T + descansoMs` e `rodadaAtual + 1`. Um DESCANSO expirado emite `DescansoTerminou` e vai para AGUARDANDO com `faseTerminaEm = null`. `faseIniciadaEm` **não** muda na pausa nem na retomada (é o início da batalha, usado no histórico).

- [ ] **Step 4: Rodar e ver passar.**

- [ ] **Step 5: Commit**: `git commit -m "feat(quest-pomodoro): máquina de estados da sessão"`

---

### Task 4: Persistência (Room + DataStore)

**Files:**
- Create: `PKG/domain/model/{BatalhaRegistro,Vitoria}.kt`, `PKG/data/local/{AppDatabase,QuestDao,BatalhaDao,SessaoDao}.kt`, `PKG/data/prefs/{Configuracoes,ConfiguracoesStore,HeroiStore}.kt`
- Test: `TST/data/ConfiguracoesTest.kt`; `app/src/androidTest/java/com/exemplo/questpomodoro/data/DaoTest.kt`

**Interfaces:**
- Consumes: `Quest`, `Sessao`, `Fase` (Tasks 2–3).
- Produces:
```kotlin
@Entity(tableName = "batalhas") data class BatalhaRegistro(@PrimaryKey(autoGenerate = true) val id: Long = 0, val questId: Long, val inicio: Long, val fim: Long, val concluida: Boolean)
data class Vitoria(val questId: Long, val titulo: String, val xpGanho: Int, val nivelAntes: Int, val nivelDepois: Int)
@Dao interface QuestDao {
    @Query("SELECT * FROM quests") fun observarTodas(): Flow<List<Quest>>
    @Query("SELECT * FROM quests WHERE id = :id") fun observar(id: Long): Flow<Quest?>
    @Query("SELECT * FROM quests WHERE id = :id") suspend fun buscar(id: Long): Quest?
    @Insert suspend fun inserir(q: Quest): Long
    @Update suspend fun atualizar(q: Quest)
    @Query("DELETE FROM quests WHERE id = :id") suspend fun excluir(id: Long)
    @Query("SELECT COUNT(*) FROM quests WHERE concluida = 0") suspend fun contarPendentes(): Int
}
@Dao interface BatalhaDao { @Insert suspend fun inserir(b: BatalhaRegistro) }
@Dao interface SessaoDao {
    @Query("SELECT * FROM sessao_ativa WHERE id = 0") fun observar(): Flow<Sessao?>
    @Query("SELECT * FROM sessao_ativa WHERE id = 0") suspend fun buscar(): Sessao?
    @Upsert suspend fun salvar(s: Sessao)
    @Query("DELETE FROM sessao_ativa") suspend fun limpar()
}
@Database(entities = [Quest::class, BatalhaRegistro::class, Sessao::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() { /* questDao(), batalhaDao(), sessaoDao() */ companion object { fun criar(context: Context): AppDatabase } }
data class Configuracoes(val focoMin: Int = 25, val descansoMin: Int = 5, val appsLiberados: Set<String> = emptySet(),
                         val lembreteAtivo: Boolean = true, val lembreteHora: Int = 19, val lembreteMinuto: Int = 0) {
    fun normalizada(): Configuracoes   // foco 1..60, descanso 1..30, hora 0..23, minuto 0..59 (coerceIn)
}
interface ConfiguracoesStore { val configuracoes: Flow<Configuracoes>; suspend fun atual(): Configuracoes; suspend fun atualizar(t: (Configuracoes) -> Configuracoes) }
interface HeroiStore {
    val xp: Flow<Int>; suspend fun xpAtual(): Int; suspend fun adicionarXp(qtd: Int)
    val vitoriaPendente: Flow<Vitoria?>; suspend fun registrarVitoria(v: Vitoria); suspend fun limparVitoria()
}
class DataStoreConfiguracoes(context: Context) : ConfiguracoesStore   // grava sempre normalizada()
class DataStoreHeroi(context: Context) : HeroiStore                    // mesmo arquivo "quest_prefs"
```

- [ ] **Step 1: Escrever `ConfiguracoesTest`**: `Configuracoes(focoMin = 0, descansoMin = 99, lembreteHora = 30).normalizada()` resulta em `focoMin == 1`, `descansoMin == 30` e `lembreteHora == 23`; e `Configuracoes().normalizada() == Configuracoes()`.
- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar** as entidades, os DAOs, `AppDatabase.criar` (nome `"quest.db"`) e os dois stores (um único `preferencesDataStore("quest_prefs")` no nível de arquivo; `appsLiberados` como `stringSetPreferencesKey`; `Vitoria` em 5 chaves, ausente quando `questId` não existe).
- [ ] **Step 4: Escrever `DaoTest`** (instrumentado, `Room.inMemoryDatabaseBuilder`): inserir uma quest e lê-la com `buscar`; `sessaoDao.salvar` duas vezes mantém uma única linha; `limpar()` faz `buscar()` retornar null; `contarPendentes` ignora concluídas.
- [ ] **Step 5: Verificar**: `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin`. Expected: PASS. Com emulador ou dispositivo conectado, rode também `./gradlew :app:connectedDebugAndroidTest`.
- [ ] **Step 6: Commit**: `git commit -m "feat(quest-pomodoro): persistência com Room e DataStore"`

---

### Task 5: Repositórios (o coração da regra de negócio)

**Files:**
- Create: `PKG/data/repository/{Transacionador,SessaoEfeitos,QuestRepository,SessaoRepository}.kt`
- Test: `TST/fakes/Fakes.kt`, `TST/data/QuestRepositoryTest.kt`, `TST/data/SessaoRepositoryTest.kt`

**Interfaces:**
- Consumes: DAOs, stores e `Configuracoes` (Task 4); `SessaoMaquina`, `Relogio` (Task 3); `QuestRegras`, `Heroi` (Task 2).
- Produces:
```kotlin
interface Transacionador { suspend fun <R> rodar(bloco: suspend () -> R): R }   // Android: db.withTransaction
sealed interface Alerta {
    data class FimDoFoco(val titulo: String, val proximaRodada: Int, val total: Int) : Alerta
    data class FimDoDescanso(val titulo: String, val proximaRodada: Int, val total: Int) : Alerta
    data class BatalhaFinal(val titulo: String) : Alerta
}
interface SessaoEfeitos { fun aoMudarSessao(sessao: Sessao?); fun alertar(alerta: Alerta) }
sealed interface ResultadoSalvar { data class Sucesso(val id: Long) : ResultadoSalvar; data class Invalido(val erros: Set<ErroQuest>) : ResultadoSalvar; data object Bloqueada : ResultadoSalvar }
class QuestRepository(questDao: QuestDao, sessaoDao: SessaoDao, relogio: Relogio) {
    fun observarQuests(): Flow<List<Quest>>; fun observarQuest(id: Long): Flow<Quest?>
    suspend fun salvar(id: Long, titulo: String, descricao: String, prioridade: Int, rodadasTotais: Int): ResultadoSalvar  // id 0 = nova
    suspend fun excluir(id: Long): Boolean
    suspend fun contarPendentes(): Int
}
class SessaoRepository(questDao: QuestDao, batalhaDao: BatalhaDao, sessaoDao: SessaoDao, heroi: HeroiStore,
                       config: ConfiguracoesStore, transacao: Transacionador, relogio: Relogio, efeitos: SessaoEfeitos) {
    val sessao: Flow<Sessao?>
    suspend fun iniciar(questId: Long): Boolean
    suspend fun pausar(); suspend fun retomar(); suspend fun pularDescanso(); suspend fun iniciarProxima()
    suspend fun desistir(); suspend fun sincronizar()
}
```

Regras do `SessaoRepository`:
- Todas as operações rodam sob um único `Mutex`; cada uma **começa aplicando `avancar(agora)`** com o mesmo tratamento de eventos do `sincronizar`.
- Eventos, aplicados dentro de `transacao.rodar`:
  - `BatalhaConcluida`: grava `rodadasConcluidas = rodada` na quest, insere `BatalhaRegistro(concluida = true)` e soma `+10 XP`.
  - `QuestConcluida`: marca a quest com `concluida = true` e `dataConclusao = fim`, soma `+50 XP` e registra `Vitoria(xpGanho = rodadasTotais * 10 + 50, nivelAntes = nivel(xp antes desta sincronização), nivelDepois = nivel(xp final))`.
- Persiste a sessão (`salvar` ou `limpar`) e chama `efeitos.aoMudarSessao(nova)` sempre que ela mudar.
- Alerta: só para o **último** evento de fim de fase da transição. `QuestConcluida` gera `BatalhaFinal`, `BatalhaConcluida` gera `FimDoFoco(proximaRodada = rodada + 1)` e `DescansoTerminou` gera `FimDoDescanso`. Sem eventos, sem alerta.
- `iniciar` retorna `false` e não faz nada se já existe sessão, se a quest não existe ou se ela está concluída. As durações vêm de `config.atual()` no momento do início.
- `desistir`: em FOCO/PAUSADO insere `BatalhaRegistro(concluida = false, inicio = faseIniciadaEm, fim = agora)`; em qualquer fase limpa a sessão; o progresso da quest não muda.
- `QuestRepository.salvar`/`excluir`: retornam `Bloqueada`/`false` se `sessaoDao.buscar()?.questId == id`. `salvar` também retorna `Bloqueada` para quest concluída. Validação com `QuestRegras.validar(titulo, rodadas, concluidasAtuais)`; grava o título com `trim()`; uma quest nova recebe `dataCriacao = relogio.agora()`.

- [ ] **Step 1: Escrever os fakes** em `Fakes.kt`: `FakeQuestDao` (sobre `MutableStateFlow<List<Quest>>`), `FakeBatalhaDao` (`val registros`), `FakeSessaoDao` (`MutableStateFlow<Sessao?>`), `FakeHeroiStore`, `FakeConfiguracoesStore(var c: Configuracoes)`, `FakeRelogio(var t: Long) : Relogio`, `FakeEfeitos` (`val sessoes = mutableListOf<Sessao?>()`, `val alertas = mutableListOf<Alerta>()`) e `TransacionadorDireto`.

- [ ] **Step 2: Escrever os testes que falham** (`runTest`; quest `Física`, 5 rodadas; foco 25, descanso 5):

```kotlin
// SessaoRepositoryTest
@Test fun iniciar_criaFoco_eNotificaEfeitos()
@Test fun iniciar_duasVezes_segundaRetornaFalse_eSessaoNaoMuda()                 // Review Focus 1
@Test fun iniciar_questConcluida_retornaFalse()
@Test fun sincronizar_fimDoFoco_contaRodada_registra_daXp_alertaFimDoFoco() {
    // relogio.t = F → quest.rodadasConcluidas == 1; batalhas == [concluida=true]; heroi.xp == 10
    // efeitos.alertas == [Alerta.FimDoFoco("Física", 2, 5)]; sessao.fase == DESCANSO
}
@Test fun sincronizar_ultimaRodada_concluiQuest_vitoria_alertaBatalhaFinal() {
    // quest 4/5 → concluida; xp == 60; vitoria == Vitoria(id, "Física", 100, 1, 1); sessao == null; alertas == [BatalhaFinal("Física")]
}
@Test fun sincronizar_concorrente_aplicaUmaVez() {                               // Review Focus 2
    // relogio.t = F; awaitAll(async { repo.sincronizar() }, async { repo.sincronizar() })
    // rodadasConcluidas == 1; heroi.xp == 10; alertas.size == 1
}
@Test fun sincronizar_semExpirar_semAlerta_semMudanca()
@Test fun sincronizar_focoEDescansoExpirados_umAlertaSo_FimDoDescanso()
@Test fun desistir_noFoco_registraNaoConcluida_mantemProgresso()
@Test fun desistir_noDescanso_naoRegistraBatalha()
@Test fun configMudadaDuranteSessao_naoAfetaSessaoAtual() {                       // Review Focus 4
    // iniciar; config.c = c.copy(focoMin = 50); relogio.t = 25min → sincronizar → fase DESCANSO
}
@Test fun pausar_depoisDoFimDoFoco_primeiroAvanca()   // relogio.t = F+1; pausar() → DESCANSO (não pausa)
// QuestRepositoryTest
@Test fun salvar_tituloEspacos_invalido()
@Test fun salvar_nova_insereComTrimEDataCriacao()
@Test fun salvar_rodadasAbaixoDoConcluido_invalido()
@Test fun salvar_questDaSessaoAtiva_bloqueada()                                  // Review Focus 5
@Test fun excluir_questDaSessaoAtiva_retornaFalse_eNaoExclui()                    // Review Focus 5
@Test fun salvar_questConcluida_bloqueada()
```

- [ ] **Step 3: Rodar e ver falhar**: `./gradlew :app:testDebugUnitTest --tests "*RepositoryTest"`.
- [ ] **Step 4: Implementar** os repositórios conforme as regras acima.
- [ ] **Step 5: Rodar e ver passar** (toda a suíte): `./gradlew :app:testDebugUnitTest`.
- [ ] **Step 6: Commit**: `git commit -m "feat(quest-pomodoro): repositórios de quest e sessão"`

---

### Task 6: Runtime Android: alarme, foreground service e notificações de fase

**Files:**
- Create: `PKG/AppContainer.kt`; `PKG/notification/{Canais,ConteudoNotificacao,Notificador}.kt`; `PKG/service/{AgendadorAlarme,SessaoEfeitosAndroid,SessaoService,FaseAlarmReceiver,SessaoAcaoReceiver,BootReceiver}.kt`
- Modify: `PKG/QuestApp.kt` (cria o container e os canais), `app/src/main/AndroidManifest.xml`, `res/values/strings.xml`
- Test: `TST/notification/ConteudoNotificacaoTest.kt`

**Interfaces:**
- Consumes: `SessaoRepository`, `QuestRepository`, `SessaoEfeitos`, `Alerta` (Task 5); `formatarTempo` (Task 3).
- Produces:
```kotlin
class AppContainer(context: Context) {
    val db: AppDatabase; val config: ConfiguracoesStore; val heroi: HeroiStore; val relogio: Relogio
    val questRepository: QuestRepository; val sessaoRepository: SessaoRepository
    val escopo: CoroutineScope      // SupervisorJob + Dispatchers.Default, para receivers e services
}
val Context.container: AppContainer   // (applicationContext as QuestApp).container
enum class AcaoNotificacao { RETOMAR, INICIAR_PROXIMA }
data class ConteudoNotificacao(val titulo: String, val texto: String, val cronometroAte: Long?, val acao: AcaoNotificacao?)
fun conteudoContinuo(s: Sessao, tituloQuest: String, agora: Long): ConteudoNotificacao
fun conteudoAlerta(a: Alerta): ConteudoNotificacao
object Canais { const val CONTINUO = "batalha"; const val ALERTAS = "alertas"; const val LEMBRETES = "lembretes"; fun criar(context: Context) }
object IdsNotificacao { const val CONTINUA = 1; const val ALERTA = 2; const val LEMBRETE = 3 }
// SessaoAcaoReceiver: intent action "com.exemplo.questpomodoro.ACAO", extra "acao" = AcaoNotificacao.name
```
Copy fixa (testada):

| Caso | título | texto | cronometroAte | ação |
|---|---|---|---|---|
| FOCO | `titulo` | `⚔️ Batalha 2/5` | `faseTerminaEm` | — |
| PAUSADO | `titulo` | `⏸ Batalha pausada — 12:40 restantes` | null | RETOMAR |
| DESCANSO | `titulo` | `🍺 Pousada — próxima: Batalha 3/5` | `faseTerminaEm` | — |
| AGUARDANDO | `titulo` | `Pronto para a Batalha 3/5` | null | INICIAR_PROXIMA |
| FimDoFoco | `Hora da Pousada! 🍺` | `Batalha 2/5 vencida em Física.` | null | — |
| FimDoDescanso | `De volta à batalha! ⚔️` | `Física — Batalha 3/5 te espera.` | null | INICIAR_PROXIMA |
| BatalhaFinal | `Vitória! 🏆` | `Última batalha de Física vencida.` | null | — |

(Em `FimDoFoco`, o número exibido é `proximaRodada - 1`.)

- [ ] **Step 1: Escrever `ConteudoNotificacaoTest`**, um teste por linha da tabela, com os valores exatos.
- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar `conteudoContinuo`/`conteudoAlerta`** e depois as peças Android:
  - `Canais.criar`: CONTINUO com `IMPORTANCE_LOW`; ALERTAS com `IMPORTANCE_HIGH`, vibração e som padrão; LEMBRETES com `IMPORTANCE_DEFAULT`.
  - `Notificador`: monta `NotificationCompat` a partir de `ConteudoNotificacao` (`setUsesChronometer(true)` + `setChronometerCountDown(true)` + `setWhen(cronometroAte)` quando não for null). O toque abre a `MainActivity` com o extra `destino = "batalha"`; a ação vira um `PendingIntent` para o `SessaoAcaoReceiver`. Ícone pequeno: vetor de espada em `res/drawable/ic_espada.xml`. Sem permissão de notificação, não posta e não quebra.
  - `AgendadorAlarme.agendar(terminaEm: Long)` / `cancelar()`: um único `PendingIntent` (request code 0) para o `FaseAlarmReceiver`. Usa `setExactAndAllowWhileIdle(RTC_WAKEUP, …)` quando `canScheduleExactAlarms()` (ou API < 31); senão, `setAndAllowWhileIdle`.
  - `SessaoEfeitosAndroid.aoMudarSessao`: agenda o alarme se `faseTerminaEm != null`, senão cancela; se a sessão não for null, chama `ContextCompat.startForegroundService(SessaoService)`. `alertar` posta em ALERTAS com `IdsNotificacao.ALERTA`.
  - `SessaoService`: chama `ServiceCompat.startForeground(..., FOREGROUND_SERVICE_TYPE_SPECIAL_USE)` em `onStartCommand`; coleta `sessaoRepository.sessao` combinado com o título da quest e atualiza a notificação `CONTINUA` a cada mudança de sessão (a contagem regressiva é do próprio cronômetro nativo; não há tique); `stopSelf()` quando a sessão vira null; `START_STICKY`, e ao reiniciar sem sessão ativa, `stopSelf()`.
  - `FaseAlarmReceiver`, `BootReceiver` e `SessaoAcaoReceiver` usam `goAsync()` + `container.escopo.launch { … ; pending.finish() }`. Os dois primeiros chamam `sincronizar()`; no boot, depois do sync, também reaplica `efeitos.aoMudarSessao(sessaoAtual)`. O de ação chama `retomar()` ou `iniciarProxima()`.
  - Manifest: `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` (`maxSdkVersion="32"`), `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`. O service usa `foregroundServiceType="specialUse"` com `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="Timer de foco pomodoro com bloqueio de distrações"/>`. Os receivers ficam com `exported="false"`, exceto o de boot.
  - `Transacionador` real em `AppContainer`: `object : Transacionador { override suspend fun <R> rodar(b: suspend () -> R) = db.withTransaction { b() } }`.
- [ ] **Step 4: Verificar**: `./gradlew :app:testDebugUnitTest :app:assembleDebug`. Expected: PASS e APK gerado.
- [ ] **Step 5: Commit**: `git commit -m "feat(quest-pomodoro): alarme de fase, foreground service e notificações"`

---

### Task 7: Tema RPG e componentes pixel

**Files:**
- Create: `app/src/main/res/font/press_start_2p.ttf`, `licenses/OFL-PressStart2P.txt`, `PKG/ui/theme/{Cores,Tipografia,Tema}.kt`, `PKG/ui/components/{PixelPanel,PixelButton,SpritePixel,Sprites,TimerCircular,BarraXp,ProgressoRodadas}.kt`
- Test: `TST/ui/SpritesTest.kt`

**Interfaces:**
- Produces:
```kotlin
object Cores { val Fundo = Color(0xFF1A1C2C); val Painel = Color(0xFF333C57); val Borda = Color(0xFFF4F4F4)
    val Ouro = Color(0xFFFFCD75); val Vermelho = Color(0xFFB13E53); val Verde = Color(0xFF38B764)
    val Azul = Color(0xFF41A6F6); val TextoSecundario = Color(0xFF94B0C2) }   // paleta Sweetie 16 (CC0)
val FontePixel: FontFamily   // R.font.press_start_2p; textos longos usam FontFamily.Default
@Composable fun TemaRpg(content: @Composable () -> Unit)   // darkColorScheme a partir de Cores; Typography: display/headline/title/label = FontePixel, body = Default
@Composable fun PixelPanel(modifier: Modifier = Modifier, borda: Color = Cores.Borda, content: @Composable ColumnScope.() -> Unit)  // cantos retos, borda dupla de 4dp
@Composable fun PixelButton(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, cor: Color = Cores.Ouro, enabled: Boolean = true, onLongClick: (() -> Unit)? = null)
data class Sprite(val linhas: List<String>, val paleta: Map<Char, Color>)   // '.' = transparente
@Composable fun SpritePixel(sprite: Sprite, modifier: Modifier = Modifier)      // Canvas, pixels quadrados, escala pelo tamanho
object Sprites { val Heroi: Sprite; val Espada: Sprite; val Fogueira: Sprite; val Caneca: Sprite; val Trofeu: Sprite; val Escudo: Sprite; val todos: List<Sprite> }  // 16×16, originais
@Composable fun TimerCircular(progresso: Float, texto: String, cor: Color, modifier: Modifier = Modifier)  // anel de 40 segmentos preenchidos conforme progresso
@Composable fun BarraXp(nivel: Int, xpNoNivel: Int, modifier: Modifier = Modifier)                        // "NV 3" + barra em blocos de 10
@Composable fun ProgressoRodadas(concluidas: Int, total: Int, modifier: Modifier = Modifier)             // blocos/corações cheios e vazios
```

- [ ] **Step 1: Baixar a fonte e a licença**

```bash
curl -L -o app/src/main/res/font/press_start_2p.ttf https://github.com/google/fonts/raw/main/ofl/pressstart2p/PressStart2P-Regular.ttf
mkdir -p licenses && curl -L -o licenses/OFL-PressStart2P.txt https://github.com/google/fonts/raw/main/ofl/pressstart2p/OFL.txt
```
Expected: `.ttf` com mais de 50 KB e licença começando com "Copyright".

- [ ] **Step 2: Escrever `SpritesTest.todosConsistentes`**: para cada sprite de `Sprites.todos`, as 16 linhas têm 16 caracteres, e todo caractere é `'.'` ou uma chave da `paleta`.
- [ ] **Step 3: Rodar e ver falhar.**
- [ ] **Step 4: Implementar** o tema, os componentes e os sprites (desenhos originais em grade de caracteres). Cada componente ganha um `@Preview`.
- [ ] **Step 5: Verificar**: `./gradlew :app:testDebugUnitTest :app:assembleDebug`. Expected: PASS.
- [ ] **Step 6: Commit**: `git commit -m "feat(quest-pomodoro): tema RPG pixel art e componentes"`

---

### Task 8: Navegação e tela da Taverna

**Files:**
- Create: `PKG/ui/Navegacao.kt`, `PKG/ui/taverna/{TavernaViewModel,TavernaScreen,VitoriaDialog}.kt`
- Modify: `PKG/MainActivity.kt` (`TemaRpg { Navegacao(destinoInicial) }`; lê o extra `destino`)
- Test: `TST/MainDispatcherRule.kt`, `TST/ui/TavernaViewModelTest.kt`

**Interfaces:**
- Consumes: `QuestRepository`, `SessaoRepository`, `HeroiStore`, `QuestRegras.filtrarEOrdenar`, `Heroi` e os componentes da Task 7.
- Produces:
```kotlin
object Rotas { const val TAVERNA = "taverna"; const val QUEST = "quest/{id}"; fun quest(id: Long) = "quest/$id"
    const val BATALHA = "batalha"; const val CONFIG = "config"; const val APPS = "config/apps" }
@Composable fun Navegacao(destinoInicial: String?)   // startDestination = TAVERNA; navega para BATALHA se destinoInicial == "batalha"
data class TavernaUiState(val nivel: Int = 1, val xpNoNivel: Int = 0, val quests: List<Quest> = emptyList(),
    val filtro: Filtro = Filtro.TODAS, val ordem: Ordem = Ordem.PRIORIDADE, val sessaoQuestId: Long? = null,
    val sessaoTitulo: String? = null, val vitoria: Vitoria? = null)
class TavernaViewModel(questRepo: QuestRepository, sessaoRepo: SessaoRepository, heroi: HeroiStore) : ViewModel() {
    val uiState: StateFlow<TavernaUiState>
    fun mudarFiltro(f: Filtro); fun mudarOrdem(o: Ordem)
    fun iniciarBatalha(questId: Long, aoIniciar: () -> Unit)   // chama aoIniciar só se iniciar() == true
    fun dispensarVitoria()
}
```
Os ViewModels são criados com `viewModel(factory = viewModelFactory { initializer { … LocalContext.current.container … } })`; vale o mesmo para as Tasks 9, 10 e 12.

- [ ] **Step 1: Escrever `MainDispatcherRule`** (`StandardTestDispatcher` + `Dispatchers.setMain`/`resetMain`) e `TavernaViewModelTest`, reaproveitando os fakes da Task 5:

```kotlin
@Test fun filtroPendentes_escondeConcluidas()
@Test fun sessaoAtiva_expoeQuestIdETitulo()
@Test fun xp250_nivel3_xpNoNivel50()
@Test fun iniciarBatalha_comSessaoAtiva_naoChamaAoIniciar()
@Test fun vitoriaPendente_apareceEDispensarLimpa()
```
- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar o ViewModel e a tela** (spec §9.1):
  - Cabeçalho do herói: `SpritePixel(Sprites.Heroi)` e `BarraXp`.
  - Banner "Batalha em andamento: <título>" (toque leva a BATALHA) quando houver sessão.
  - Chips de filtro e um seletor de ordem.
  - `LazyColumn` de `PixelPanel`, cada um com título, `Prioridade.raridade` colorida (Comum = TextoSecundario, Rara = Azul, Épica = Ouro), `ProgressoRodadas` e o botão ⚔️. O ⚔️ fica desabilitado se houver sessão ou se a quest estiver concluída. O toque no card abre `quest(id)`.
  - FAB "Nova Quest" → `quest(0)`; ícone de engrenagem → CONFIG.
  - `VitoriaDialog` em tela cheia com `Sprites.Trofeu`, "Quest concluída!", "+N XP" e "LEVEL UP! NV x" quando `nivelDepois > nivelAntes`.
  - No primeiro carregamento, em API ≥ 33, pede `POST_NOTIFICATIONS` com `rememberLauncherForActivityResult`.
  - Por enquanto, o ⚔️ chama `iniciarBatalha` direto. O diálogo de acessibilidade entra na Task 11.
- [ ] **Step 4: Verificar**: `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
- [ ] **Step 5: Commit**: `git commit -m "feat(quest-pomodoro): navegação e tela da Taverna"`

---

### Task 9: Criar/editar Quest

**Files:**
- Create: `PKG/ui/quest/{QuestFormViewModel,QuestFormScreen}.kt`
- Test: `TST/ui/QuestFormViewModelTest.kt`

**Interfaces:**
- Consumes: `QuestRepository.salvar/excluir/observarQuest`, `SessaoRepository.sessao`, `ConfiguracoesStore`, `QuestRegras.textoPrevia`, `ErroQuest`, `ResultadoSalvar`.
- Produces:
```kotlin
data class QuestFormUiState(val titulo: String = "", val descricao: String = "", val prioridade: Prioridade = Prioridade.MEDIA,
    val rodadas: Int = 4, val rodadasConcluidas: Int = 0, val previa: String = "", val erros: Set<ErroQuest> = emptySet(),
    val editando: Boolean = false, val somenteLeitura: Boolean = false, val concluido: Boolean = false)  // concluido = salvou ou excluiu → voltar
class QuestFormViewModel(questId: Long, questRepo: QuestRepository, sessaoRepo: SessaoRepository, config: ConfiguracoesStore) : ViewModel() {
    val uiState: StateFlow<QuestFormUiState>
    fun mudarTitulo(t: String); fun mudarDescricao(d: String); fun mudarPrioridade(p: Prioridade)
    fun mudarRodadas(n: Int)   // coerceIn(max(1, rodadasConcluidas), 20)
    fun salvar(); fun excluir()
}
```
Mensagens (`strings.xml`): TITULO_VAZIO "Dê um nome à sua quest.", TITULO_LONGO "Máximo de 60 caracteres.", RODADAS_FORA_DO_INTERVALO "Entre 1 e 20 batalhas.", RODADAS_ABAIXO_DO_CONCLUIDO "Já foram vencidas %d batalhas." `somenteLeitura` vale quando a quest está concluída ou é a da sessão ativa; nesse caso a tela mostra o aviso "Quest concluída — só é possível excluir." ou "Em batalha — termine ou desista para editar." *(Rodadas padrão = 4 é decisão do Claude.)*

- [ ] **Step 1: Escrever os testes**:

```kotlin
@Test fun previaUsaDuracoesDaConfig()  // config foco 30, descanso 10; rodadas 3 → "3 batalhas = 90 min de foco + 20 min de pousada"
@Test fun salvarTituloVazio_mostraErro_naoConclui()
@Test fun salvarValido_concluido()
@Test fun editarQuestConcluida_somenteLeitura()
@Test fun mudarRodadas_naoDesceAbaixoDasConcluidas()   // concluidas 3; mudarRodadas(1) → 3
```
- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar** o ViewModel e a tela (spec §9.2): `OutlinedTextField` com contador `n/60`; prioridade em 3 botões de raridade; stepper − / número / + para as rodadas; prévia num `PixelPanel`; "Salvar"; "Excluir" (só ao editar) com `AlertDialog` "Excluir esta quest? O progresso será perdido." Se `excluir` retornar false, o snackbar diz "Em batalha — não dá para excluir agora."
- [ ] **Step 4: Verificar**: `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
- [ ] **Step 5: Commit**: `git commit -m "feat(quest-pomodoro): criar e editar quest"`

---

### Task 10: Tela de Batalha e Pousada

**Files:**
- Create: `PKG/ui/batalha/{BatalhaViewModel,BatalhaScreen,ConfirmarDesistencia}.kt`
- Test: `TST/ui/BatalhaViewModelTest.kt`

**Interfaces:**
- Consumes: `SessaoRepository` (todas as ações e `sessao`), `QuestRepository.observarQuest`, `Relogio`, `SessaoMaquina.restanteMs`, `formatarTempo`.
- Produces:
```kotlin
data class BatalhaUiState(val fase: Fase? = null, val titulo: String = "", val rodadaAtual: Int = 0, val rodadasTotais: Int = 0,
    val restanteMs: Long = 0, val progresso: Float = 0f, val encerrada: Boolean = false)  // encerrada = sessão null → voltar para a Taverna
class BatalhaViewModel(sessaoRepo: SessaoRepository, questRepo: QuestRepository, relogio: Relogio,
                       tique: Flow<Unit> = flow { while (true) { emit(Unit); delay(1000) } }) : ViewModel() {
    val uiState: StateFlow<BatalhaUiState>
    fun pausar(); fun retomar(); fun pularDescanso(); fun iniciarProxima(); fun desistir()
}
fun textoConfirmacaoDesistir(fase: Fase, rodadaAtual: Int, total: Int): String
```
- `progresso` = `1 - restanteMs / duraçãoDaFase` (FOCO/PAUSADO usam `focoMs`, DESCANSO usa `descansoMs`); AGUARDANDO = 0.
- Quando `restanteMs == 0` em FOCO ou DESCANSO, chama `sessaoRepo.sincronizar()` **uma vez por `faseTerminaEm`** (guarda o último valor sincronizado).
- `textoConfirmacaoDesistir`: FOCO/PAUSADO → `"Tem certeza? A batalha atual não vai contar."`; DESCANSO/AGUARDANDO → `"Sair da quest? Seu progresso (${rodadaAtual - 1}/$total) fica salvo."`

- [ ] **Step 1: Escrever os testes** (com `tique = MutableSharedFlow<Unit>()` e `FakeRelogio`):

```kotlin
@Test fun foco_restanteEProgresso()        // t = 5 min → restante 20 min, progresso 0.2f
@Test fun pausado_restanteCongelado()
@Test fun chegarAZero_sincronizaSemAlarme() // t = fim do foco, emite 3 tiques → fase vira DESCANSO e efeitos.alertas.size == 1
@Test fun sessaoNula_encerrada()
@Test fun textoDesistir_porFase()          // FOCO e DESCANSO(rodadaAtual 3, total 5) → "…(2/5)…"
```
- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar a tela** (spec §9.3–9.4), um `when (fase)`:
  - FOCO/PAUSADO: fundo Fundo; `SpritePixel(Sprites.Espada)`; "Batalha 2/5"; `TimerCircular(cor = Vermelho)` com `formatarTempo`; "Pausar"/"Retomar"; "Desistir" → `ConfirmarDesistencia`. Pausado, mostra o selo "⏸ Pausado — apps liberados".
  - DESCANSO: `Sprites.Fogueira` + `Sprites.Caneca`; `TimerCircular(cor = Azul)`; "Próxima: Batalha 3/5"; "Pular descanso"; "Desistir".
  - AGUARDANDO: botão grande "Iniciar Batalha 3/5" e "Sair da quest".
  - `encerrada` → `navController.popBackStack(Rotas.TAVERNA, false)`.
  - `BackHandler` volta para a Taverna sem desistir.
- [ ] **Step 4: Verificar**: `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
- [ ] **Step 5: Commit**: `git commit -m "feat(quest-pomodoro): telas de batalha e pousada"`

---

### Task 11: Bloqueio de apps (Acessibilidade)

**Files:**
- Create: `PKG/domain/RegrasBloqueio.kt`, `PKG/service/{BloqueioAccessibilityService,SempreLiberados}.kt`, `PKG/ui/bloqueio/{BloqueioActivity,BloqueioScreen}.kt`, `PKG/ui/components/AvisoBloqueio.kt`, `res/xml/bloqueio_accessibility.xml`
- Modify: `AndroidManifest.xml`; `TavernaScreen.kt` (diálogo antes da batalha); `BatalhaScreen.kt` (aviso)
- Test: `TST/domain/RegrasBloqueioTest.kt`

**Interfaces:**
- Consumes: `SessaoRepository.sessao/desistir`, `ConfiguracoesStore.configuracoes`, `Fase`, `formatarTempo`, `textoConfirmacaoDesistir` (Task 10).
- Produces:
```kotlin
val PACOTES_SISTEMA_LIBERADOS: Set<String> = setOf("com.android.systemui", "com.android.settings", "com.android.phone",
    "com.android.server.telecom", "com.android.incallui", "com.android.dialer", "com.google.android.dialer",
    "com.android.emergency", "com.google.android.apps.safetyhub", "com.android.permissioncontroller", "com.google.android.permissioncontroller")
fun deveBloquear(pacote: String?, fase: Fase?, liberados: Set<String>, sempreLiberados: Set<String>): Boolean
fun servicoAcessibilidadeAtivo(valorConfig: String?, pacote: String, classe: String): Boolean
fun sempreLiberados(context: Context): Set<String>    // PACOTES_SISTEMA_LIBERADOS + o próprio app + discador padrão (TelecomManager) + launcher(s) HOME + pacote do IME padrão
object BloqueioStatus { fun ativo(context: Context): Boolean }   // lê Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
```

- [ ] **Step 1: Escrever os testes**:

```kotlin
@Test fun bloqueiaAppComumNoFoco() = assertTrue(deveBloquear("com.instagram.android", Fase.FOCO, emptySet(), setOf("com.exemplo.questpomodoro")))
@Test fun naoBloqueiaForaDoFoco() { listOf(Fase.PAUSADO, Fase.DESCANSO, Fase.AGUARDANDO, null).forEach { assertFalse(deveBloquear("com.instagram.android", it, emptySet(), emptySet())) } }
@Test fun naoBloqueiaLiberadoPeloUsuario()
@Test fun naoBloqueiaTelaDeChamada() { listOf("com.android.incallui", "com.google.android.dialer", "com.android.server.telecom").forEach { assertFalse(deveBloquear(it, Fase.FOCO, emptySet(), PACOTES_SISTEMA_LIBERADOS)) } }  // Review Focus 3
@Test fun pacoteNulo_naoBloqueia()
@Test fun servicoAtivo_formaCompletaECurta() {
    val c = "com.exemplo.questpomodoro.service.BloqueioAccessibilityService"
    assertTrue(servicoAcessibilidadeAtivo("x/y:com.exemplo.questpomodoro/$c", "com.exemplo.questpomodoro", c))
    assertTrue(servicoAcessibilidadeAtivo("com.exemplo.questpomodoro/.service.BloqueioAccessibilityService", "com.exemplo.questpomodoro", c))
    assertFalse(servicoAcessibilidadeAtivo(null, "com.exemplo.questpomodoro", c)) }
```
- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar as funções puras.**
  - `deveBloquear` só retorna true quando `fase == FOCO`, `pacote != null`, o pacote não está em `liberados ∪ sempreLiberados` e não começa com `"com.android.systemui"`.
  - `servicoAcessibilidadeAtivo`: separa o valor por `:`; cada item por `/`; a classe pode ser completa ou começar com `.` (relativa ao pacote); a comparação ignora maiúsculas e minúsculas.
- [ ] **Step 4: Implementar o serviço e a tela de bloqueio.**
  - `BloqueioAccessibilityService`: em `onServiceConnected`, coleta `sessao` e `configuracoes` em campos `@Volatile` e calcula `sempreLiberados(this)`. Em `onAccessibilityEvent` com `TYPE_WINDOW_STATE_CHANGED`, se `deveBloquear(...)`, abre a `BloqueioActivity` com `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP`.
  - `bloqueio_accessibility.xml`: `accessibilityEventTypes="typeWindowStateChanged"`, `accessibilityFeedbackType="feedbackGeneric"`, `canRetrieveWindowContent="false"`, `notificationTimeout="100"`, com descrição em pt-BR explicando que o serviço só observa qual app abriu, para bloquear durante o foco.
  - Manifest: o service com `BIND_ACCESSIBILITY_SERVICE` e `exported="true"`; a `BloqueioActivity` com `launchMode="singleTask"`, `excludeFromRecents="true"` e `taskAffinity=""`.
  - `BloqueioScreen`: `Sprites.Escudo`, "Você não pode fugir da batalha!", título da quest e tempo restante (tique de 1 s). "Voltar à batalha" (e também o botão Voltar do sistema) abre a `MainActivity` com `destino = "batalha"` e dá `finish()`. "Desistir" abre o `ConfirmarDesistencia` da Task 10 e chama `desistir()`. Quando a sessão sai do FOCO, a tela se fecha sozinha.
- [ ] **Step 5: Ligar o fluxo na interface.**
  - Taverna: o ⚔️ chama `BloqueioStatus.ativo(context)`. Se false, mostra um diálogo temático "Ative o Escudo Anti-Distração" com "Ativar bloqueio" (`Settings.ACTION_ACCESSIBILITY_SETTINGS`) e "Batalhar sem bloqueio" (inicia mesmo assim).
  - `BatalhaScreen`: mostra `AvisoBloqueio` ("⚠ Bloqueio desativado — outros apps estão liberados") no FOCO quando inativo, reavaliado em `LifecycleResumeEffect`.
- [ ] **Step 6: Verificar**: `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
- [ ] **Step 7: Commit**: `git commit -m "feat(quest-pomodoro): bloqueio de apps por acessibilidade"`

---

### Task 12: Configurações, apps liberados e lembrete diário

**Files:**
- Create: `PKG/domain/Lembrete.kt`, `PKG/notification/{LembreteWorker,LembreteAgendador}.kt`, `PKG/ui/config/{ConfiguracoesViewModel,ConfiguracoesScreen,AppsLiberadosScreen}.kt`
- Modify: `AndroidManifest.xml` (`<queries>` com intent MAIN/LAUNCHER), `QuestApp.kt` (`LembreteAgendador.reagendar` ao iniciar), `Navegacao.kt`
- Test: `TST/domain/LembreteTest.kt`, `TST/ui/ConfiguracoesViewModelTest.kt`

**Interfaces:**
- Consumes: `ConfiguracoesStore`, `QuestRepository.contarPendentes`, `Canais.LEMBRETES`, `IdsNotificacao.LEMBRETE`, `BloqueioStatus` (Task 11).
- Produces:
```kotlin
fun atrasoAteProximo(agora: LocalDateTime, hora: Int, minuto: Int): Duration   // hoje se ainda não passou (estritamente depois de agora), senão amanhã
fun textoLembrete(pendentes: Int): String?   // 0 → null; 1 → "Sua party te espera: 1 quest pendente."; n → "Sua party te espera: n quests pendentes."
object LembreteAgendador { fun reagendar(context: Context, c: Configuracoes) }   // unique work "lembrete", REPLACE; cancela se !lembreteAtivo
class LembreteWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params)   // posta se textoLembrete != null e depois chama reagendar
data class AppInstalado(val pacote: String, val nome: String)
class ConfiguracoesViewModel(config: ConfiguracoesStore, aoMudarLembrete: (Configuracoes) -> Unit) : ViewModel() {
    val uiState: StateFlow<Configuracoes>
    fun mudarFoco(min: Int); fun mudarDescanso(min: Int); fun alternarApp(pacote: String)
    fun mudarLembrete(ativo: Boolean, hora: Int, minuto: Int)   // grava e chama aoMudarLembrete(novo)
}
```

- [ ] **Step 1: Escrever os testes**:

```kotlin
@Test fun antesDoHorario_hoje() = assertEquals(Duration.ofHours(2), atrasoAteProximo(LocalDateTime.of(2026, 11, 2, 17, 0), 19, 0))
@Test fun depoisDoHorario_amanha() = assertEquals(Duration.ofHours(23), atrasoAteProximo(LocalDateTime.of(2026, 11, 2, 20, 0), 19, 0))
@Test fun exatamenteNoHorario_amanha() = assertEquals(Duration.ofHours(24), atrasoAteProximo(LocalDateTime.of(2026, 11, 2, 19, 0), 19, 0))
@Test fun textos() { assertNull(textoLembrete(0)); assertEquals("Sua party te espera: 1 quest pendente.", textoLembrete(1)); assertEquals("Sua party te espera: 3 quests pendentes.", textoLembrete(3)) }
@Test fun vm_focoForaDaFaixa_normaliza()        // mudarFoco(90) → 60
@Test fun vm_alternarApp_adicionaERemove()
@Test fun vm_mudarLembrete_chamaCallback()
```
- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar.**
  - Configurações (spec §9.6): steppers de foco e de descanso, com a nota "Vale a partir da próxima batalha."; o item "Apps liberados (N)" leva a APPS; um switch e um `TimePicker` para o lembrete; o status das permissões (acessibilidade e notificações), com os atalhos `ACTION_ACCESSIBILITY_SETTINGS` e `ACTION_APP_NOTIFICATION_SETTINGS`.
  - `AppsLiberadosScreen`: `queryIntentActivities(MAIN/LAUNCHER)`, sem o próprio app, ordenada por nome, com ícone, nome e checkbox. Um texto fixo no topo diz "Telefone e emergência sempre liberados."
- [ ] **Step 4: Verificar**: `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
- [ ] **Step 5: Commit**: `git commit -m "feat(quest-pomodoro): configurações, apps liberados e lembrete diário"`

---

### Task 13: Documentação e verificação manual

**Files:**
- Create: `05-06-quest-pomodoro/README.md`, `05-06-quest-pomodoro/screenshots/*.png`
- Modify: `README.md` da raiz (seção "Projetos 05 + 06 — Quest Pomodoro" com um link para a pasta)

- [ ] **Step 1: Escrever o README da pasta**: o que o app faz, como abrir e rodar (Android Studio ou `./gradlew installDebug`), as permissões e o motivo de cada uma, como ativar a acessibilidade e os créditos (Press Start 2P/OFL, Sweetie 16/CC0).
- [ ] **Step 2: Fazer o roteiro manual em emulador API 36** (`system-images/android-36.1`), com foco e descanso de 1 min:
  1. Criar "Estudar Física" com 3 rodadas → a prévia diz "3 batalhas = 3 min de foco + 2 min de pousada".
  2. Iniciar com a acessibilidade ativa, abrir o Chrome → aparece a tela de bloqueio; abrir o discador → liberado.
  3. Pausar → o Chrome abre; retomar → volta a bloquear.
  4. Apagar a tela até o fim do foco → toca o alerta "Hora da Pousada!" na hora; a notificação contínua conta o descanso.
  5. Fim do descanso → alerta "De volta à batalha!", estado AGUARDANDO; tocar em "Iniciar" na notificação começa a batalha.
  6. Girar a tela durante o foco → o tempo não reinicia.
  7. `adb shell am kill com.exemplo.questpomodoro` durante o foco e reabrir depois do fim → a rodada foi contada.
  8. Desistir na batalha 3 → o progresso fica 2/3 e a sessão acaba.
  9. Concluir a quest → `VitoriaDialog` com +80 XP.
  10. Lembrete: configurar 1 min à frente com uma quest pendente → a notificação chega.
- [ ] **Step 3: Fazer as capturas de tela** (Taverna, Formulário, Batalha, Pousada, Bloqueio, Vitória e Configurações) para `screenshots/`, com `adb exec-out screencap -p > screenshots/<tela>.png`.
- [ ] **Step 4: Rodar a suíte final**: `./gradlew :app:testDebugUnitTest :app:assembleDebug`. Expected: PASS.
- [ ] **Step 5: Commit**: `git commit -m "docs(quest-pomodoro): README, capturas e roteiro de teste"`
