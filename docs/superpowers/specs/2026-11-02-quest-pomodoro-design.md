# Quest Pomodoro — Design

> Junção dos Projetos 05 (Pomodoro) e 06 (Gerenciador de Tarefas) do Android Dev Lab,
> com tema RPG pixel art, bloqueio de apps e notificações.

**Pasta:** `11-quest-pomodoro/` *(decisão do Claude)*
**Data:** 2026-11-02

---

## 1. Intenção

O usuário cadastra uma tarefa e diz quantas rodadas pomodoro ela exige.
Exemplo: *"Estudar Física — 5 rodadas"* = 5 × 25 min de foco, com descansos separados entre elas.
Durante o foco, o celular bloqueia os outros apps para manter o usuário na tarefa.
O app avisa por notificação quando cada fase termina e lembra todo dia das tarefas pendentes.

A interface é um **RPG pixel art**: tarefas são **Quests**, cada rodada de foco é uma **Batalha**,
o descanso é a **Pousada**, e concluir rodadas dá **XP** ao herói, que sobe de nível.

### Por quê
- Juntar o "o que fazer" (lista de tarefas) com o "quanto tempo dedicar" (pomodoro) num único fluxo.
- Bloquear distrações de verdade durante o foco, e não só cronometrar.
- Tornar a rotina de estudo divertida com a gamificação.

---

## 2. Objetivos, não-objetivos e anti-objetivos

### Objetivos
- CRUD completo de Quests com persistência local (Room), como no Projeto 06.
- Cada Quest tem um número de rodadas; o progresso (ex.: 2/5) fica salvo e pode ser retomado depois.
- Um timer pomodoro estável, que não acelera nem reinicia por recomposição, rotação, tela apagada
  ou morte do processo.
- Bloqueio dos apps não liberados durante o foco, via Serviço de Acessibilidade.
- Notificações: contagem em andamento, fim do foco/descanso e lembrete diário.
- Tema RPG pixel art consistente em todas as telas.

### Não-objetivos
- Impedir burlas de usuários determinados, como desligar a acessibilidade nas configurações
  ou forçar a parada do app. O bloqueio serve para criar atrito, não para ser uma prisão.
- Sincronização na nuvem, login ou várias contas.
- Publicação na Play Store (as políticas de Acessibilidade e de alarme exato não são uma preocupação aqui).
- Notificação de "quest concluída" (o usuário não escolheu essa opção; a vitória aparece só dentro do app).

### Anti-objetivos (fracasso mesmo que "funcione")
- O usuário ficar **preso**: não conseguir sair do bloqueio, ligar para alguém ou desativar o pomodoro.
- O timer derivar ou perder o fim da fase com a tela apagada.
- Usar sprites, músicas ou personagens de franquias protegidas. Todo o visual deve ser original ou de licença livre.

---

## 3. Decisões do usuário

| Tema | Decisão |
|---|---|
| Bloqueio | **Serviço de Acessibilidade**: detecta o app em primeiro plano e leva à tela de bloqueio temática. |
| Apps liberados | Telefone e emergência **sempre** liberados; o usuário escolhe outros numa lista de apps instalados. |
| Tema | **RPG pixel art**: Quests, Batalhas, Pousada, XP e nível. |
| Desativar pomodoro | **Confirmação simples**: botão "Desistir" → "Tem certeza?" → para. A rodada em andamento não conta. **Sem penalidade** de XP. |
| Durações | **Configuráveis** em Configurações (padrão: foco 25 min, descanso 5 min), salvas com DataStore. Sem descanso longo. |
| Última rodada | **Sem descanso final**: ao fim da última batalha, a quest é concluída na hora. 5 rodadas de 25/5 = 125 min de foco + 20 min de descanso. |
| Notificações | Contagem em andamento · Fim do foco/descanso · Lembrete diário. |
| Progresso | **Salvo por rodada**: dá para fazer 2/5 hoje e 3/5 amanhã. Desistir descarta só a rodada em andamento. |
| Pausa | O foco **pode ser pausado**, e **durante a pausa os apps ficam liberados**. Retomar volta a bloquear. |
| Próxima rodada | Ao fim do descanso, **espera o usuário** tocar em "Iniciar Batalha N/T". O foco não começa sozinho. |

---

## 4. Stack *(decisões do Claude, padrão do README)*

- Kotlin + Jetpack Compose + Material 3 com tema customizado.
- `minSdk 26` (canais de notificação), `targetSdk`/`compileSdk` na versão estável mais recente.
- Arquitetura MVVM: `ViewModel` + `StateFlow`, Repository, Room, DataStore e Navigation Compose.
- Estrutura de pastas igual à "Organização Interna Recomendada" do README (`data/`, `domain/`, `ui/`, `viewmodel/`),
  acrescentando `service/` (timer e acessibilidade) e `notification/`.
- Textos em **pt-BR** em `strings.xml`.

---

## 5. Conceitos do domínio

### Quest
| Campo | Tipo | Observação |
|---|---|---|
| id | Long | PK autoincrement |
| titulo | String | obrigatório, até 60 caracteres |
| descricao | String | opcional |
| prioridade | Int | 1 = Baixa, 2 = Média, 3 = Alta (mostrada como raridade: Comum/Rara/Épica) |
| rodadasTotais | Int | 1–20 |
| rodadasConcluidas | Int | 0..rodadasTotais |
| concluida | Boolean | true quando rodadasConcluidas == rodadasTotais |
| dataCriacao | Long | epoch millis |
| dataConclusao | Long? | epoch millis |

### Sessão ativa (persistida: Room ou DataStore, a critério do builder)
Só existe **uma** sessão ativa por vez. Ela guarda:
`questId`, `fase` (FOCO | PAUSADO | DESCANSO | AGUARDANDO), `rodadaAtual`, `faseIniciadaEm`,
`faseTerminaEm` (epoch millis, quando rodando), `restanteAoPausarMs` (quando pausado)
e as durações capturadas no início da sessão (mudar as Configurações no meio de uma sessão não a afeta).

> **Regra do timer:** rodando, o tempo restante é sempre `faseTerminaEm − agora`, e nunca um contador decrementado.
> Pausar salva `restanteAoPausarMs` e cancela o alarme de fim de fase; retomar define
> `faseTerminaEm = agora + restanteAoPausarMs` e reagenda o alarme.
> Isso resolve recomposição, rotação, tela apagada e morte do processo: ao reabrir, o estado é recalculado
> a partir do horário salvo. Se o processo morreu e a fase já passou, a sessão avança para o estado correto
> (FOCO expirado → conta a rodada e vai para DESCANSO ou VITÓRIA; DESCANSO expirado → AGUARDANDO).

### Histórico de batalhas
`BatalhaRegistro(id, questId, inicio, fim, concluida: Boolean)` registra cada rodada,
inclusive as desistências. Ele alimenta as estatísticas e o XP.

### Herói *(regras do Claude)*
- **+10 XP** por batalha concluída; **+50 XP** de bônus ao concluir uma quest.
- Nível = `1 + XP / 100` (cada 100 XP = 1 nível). XP e nível ficam no DataStore.
- Desistir não tira XP.

---

## 6. Fluxo da sessão (máquina de estados)

```
           [Iniciar batalha]                 [Pausar]
OCIOSO ─────────────────────► FOCO (rodada N) ◄──────► PAUSADO   (apps liberados na pausa)
  ▲                             │ fim do tempo  [Retomar]
  │                             ▼
  │               rodadaAtual == total? ──sim──► VITÓRIA (quest concluída, +XP) ──► OCIOSO
  │                             │ não
  │                             ▼
  │                         DESCANSO ──fim do tempo / Pular──► AGUARDANDO
  │                                                              │ [Iniciar Batalha N+1/T]
  │                                                              ▼
  │                                                         FOCO (rodada N+1)
  │
  └──── Desistir (confirmado) ── de FOCO, PAUSADO, DESCANSO ou AGUARDANDO
```

- **Ao concluir o FOCO:** `rodadasConcluidas++`, +10 XP, registro no histórico e alerta "Hora da Pousada!".
  O DESCANSO começa automaticamente.
- **Pausa (só no FOCO):** congela o tempo restante e **libera os apps**. Sem limite de pausas nem de duração
  *(decisão do Claude)*. A notificação contínua mostra "⏸ Batalha pausada — 12:40 restantes", com a ação "Retomar".
  Retomar volta a bloquear. O DESCANSO não tem pausa, porque já existe o "Pular descanso".
- **Durante o DESCANSO:** os apps ficam liberados. Botão **"Pular descanso"** *(decisão do Claude)* → AGUARDANDO.
- **Ao fim do DESCANSO:** alerta "De volta à batalha!" → **AGUARDANDO**. Os apps continuam liberados até o usuário
  tocar em "Iniciar Batalha N/T" (na tela ou na ação da notificação). Nada começa sozinho.
- **Desistir:** confirmação "Tem certeza? A batalha atual não vai contar." → sessão encerrada, a rodada em
  andamento não conta, e o progresso já concluído é mantido. Em AGUARDANDO, "Desistir" vira "Sair da quest"
  (nenhuma rodada é perdida).
- Enquanto existir uma sessão ativa, a quest dela não pode ser editada nem excluída, e outra sessão não pode começar.

---

## 7. Bloqueio de apps

- `AccessibilityService` que escuta `TYPE_WINDOW_STATE_CHANGED`.
- Quando a fase é **FOCO** e o pacote em primeiro plano **não** está liberado → abre a `BloqueioActivity`
  (tela cheia, temática: *"Você não pode fugir da batalha!"*, com tempo restante, botão **"Voltar à batalha"**
  que leva ao app e botão **"Desistir"** com a mesma confirmação).
- **Sempre liberados:** o próprio app, o discador padrão/telecom e as chamadas de emergência, a interface do sistema
  (SystemUI), o launcher, o teclado (IME) e as Configurações *(decisão do Claude: liberar launcher e Configurações
  evita prender o usuário; o bloqueio é por app aberto)*.
- **Liberados pelo usuário:** tela em Configurações com a lista de apps instaláveis/lançáveis (ícone + nome + checkbox),
  salva no DataStore. No Android 11+ é preciso declarar `<queries>` com a intent de launcher.
- **Permissão:** onboarding temático explicando por que a Acessibilidade é necessária, com um botão que abre
  as configurações de acessibilidade. Se o usuário não ativar, a batalha **pode** começar sem bloqueio,
  com um aviso visível na tela de batalha *(decisão do Claude)*.
- O serviço **só bloqueia na fase FOCO rodando**. Em PAUSADO, DESCANSO, AGUARDANDO ou sem sessão, ele não faz nada.

---

## 8. Notificações

| Canal | Importância | Conteúdo |
|---|---|---|
| **Batalha em andamento** | Baixa, contínua (foreground service) | "Estudar Física — Batalha 2/5 — 14:32", com contagem regressiva nativa (`setUsesChronometer` + `setChronometerCountDown`) e toque que abre a tela da batalha. Também aparece no descanso ("Pousada — 3:10"), na pausa ("⏸ Batalha pausada", ação "Retomar") e em AGUARDANDO ("Pronto para a Batalha 3/5", ação "Iniciar"). |
| **Alertas de fase** | Alta (som + vibração) | Fim do foco: "Hora da Pousada! 🍺". Fim do descanso: "De volta à batalha! ⚔️". |
| **Lembrete diário** | Padrão | Num horário configurável (padrão 19:00, liga/desliga), **só se houver quests pendentes**: "Sua party te espera: 3 quests pendentes." |

- O fim de cada fase precisa disparar **na hora certa mesmo com a tela apagada/Doze**. Recomendação: alarme exato
  (`AlarmManager.setExactAndAllowWhileIdle`) para o fim da fase, mais um foreground service para a notificação
  contínua. A escolha final da técnica fica com o builder, mas a precisão é obrigatória.
- Permissões: `POST_NOTIFICATIONS` (13+), em tempo de execução; `FOREGROUND_SERVICE` + o tipo adequado no 14+
  (ex.: `specialUse`); e as permissões de alarme exato, se esse caminho for usado.
- Lembrete diário: WorkManager ou AlarmManager, a critério do builder, reagendado após o boot.

---

## 9. Telas (tema RPG pixel art)

1. **Taverna (início):** cabeçalho do herói (avatar pixel, nível, barra de XP); lista de Quests (`LazyColumn`)
   com raridade/prioridade, barra de progresso em "corações" ou blocos (2/5) e botão ⚔️ para iniciar;
   filtros **Todas / Pendentes / Concluídas**; ordenação por prioridade (padrão) ou data; FAB "Nova Quest".
2. **Criar/Editar Quest:** título, descrição, prioridade e número de rodadas (stepper 1–20) com uma prévia calculada:
   *"5 batalhas = 125 min de foco + 20 min de pousada"*. Validação: título obrigatório; ao editar,
   as rodadas não podem ficar abaixo de `rodadasConcluidas`. Excluir com confirmação.
3. **Batalha (foco):** nome da quest, "Batalha 2/5", indicador circular de progresso em estilo pixel com o tempo
   restante, aviso caso o bloqueio esteja inativo e botões "Pausar"/"Retomar" e "Desistir".
   Pausado, mostra o estado "⏸ Pausado — apps liberados".
4. **Pousada (descanso):** visual de taverna/fogueira, tempo restante, "Próxima: Batalha 3/5",
   botões "Pular descanso" e "Desistir". Quando o descanso acaba, a mesma tela vira o estado AGUARDANDO,
   com o botão grande "Iniciar Batalha 3/5" e "Sair da quest".
5. **Vitória:** quest concluída, XP ganho, animação de level up se houver.
6. **Configurações:** duração do foco (1–60 min) e do descanso (1–30 min); apps liberados; lembrete diário
   (liga/desliga + horário); status das permissões (acessibilidade, notificações) com atalhos.
7. **Bloqueio:** a `BloqueioActivity` descrita na seção 7.

**Diretrizes visuais**
- Fonte pixel com licença livre (ex.: *Press Start 2P*, OFL) para títulos e números; uma fonte legível para textos longos.
- Paleta retrô de 16 bits definida como tokens no tema do Compose (fundo escuro, molduras estilo caixa de diálogo de RPG).
- Ícones e sprites originais ou de licença livre (seção 2, anti-objetivos).
- O texto precisa continuar legível em telas pequenas e com fonte do sistema ampliada.

---

## 10. Testes e qualidade

- **Testes unitários (obrigatórios)** para a regra de negócio pura:
  máquina de estados da sessão (incluindo pausar/retomar e AGUARDANDO), cálculo do tempo restante a partir
  de timestamps, recuperação após a morte do processo (fase expirada → próximo estado), cálculo de XP/nível, prévia "N batalhas = X min + Y min" e validações da Quest.
- Checklist do README (compila, sem cortes de interface, mensagens de erro compreensíveis, capturas de tela,
  instruções de execução no README da pasta).
- Teste manual em emulador/dispositivo: tela apagada durante o foco, rotação, abrir um app bloqueado,
  abrir um app liberado, ligação recebida durante o foco.

---

## 11. Deixado para o builder

- Onde persistir a sessão ativa (tabela Room ou DataStore).
- Técnica exata para disparar o fim da fase (desde que seja precisa com a tela apagada).
- WorkManager ou AlarmManager para o lembrete diário.
- Arte pixel concreta, animações e paleta exata, dentro das diretrizes da seção 9.
- Se vai usar injeção de dependência (Hilt) ou criação manual.
- Quests concluídas ficam no filtro "Concluídas" e podem ser excluídas; não existe "refazer quest"
  *(decisão do Claude)*.
