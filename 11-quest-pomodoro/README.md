# ⚔️ Quest Pomodoro

**Projeto extra do Android Dev Lab**: a junção dos Projetos 05 (Pomodoro) e 06 (Gerenciador de Tarefas), com tema **RPG pixel art**, **bloqueio de apps** durante o foco e **notificações**.

Você cadastra uma **Quest** (tarefa) e diz quantas **batalhas** (rodadas pomodoro) ela precisa.
Exemplo: *Estudar Física — 5 batalhas* = 5 × 25 min de foco, com 5 min de **Pousada** (descanso) entre elas.
Cada batalha vencida dá **+10 XP**, concluir a quest dá **+50 XP**, e o herói sobe de nível a cada 100 XP.

## Funcionalidades

| Área | O que faz |
|---|---|
| Quests | Criar, editar e excluir; prioridade como raridade (Comum/Rara/Épica); filtros Todas/Pendentes/Concluídas; ordenação por raridade ou data |
| Progresso | Salvo por rodada: dá para fazer 2/5 hoje e 3/5 amanhã |
| Batalha (foco) | Timer circular pixel; **Pausar** (libera os apps) / **Retomar**; **Desistir** com confirmação (a batalha atual não conta) |
| Pousada (descanso) | Apps liberados; **Pular descanso**; ao terminar, espera você tocar em "Iniciar Batalha" |
| Bloqueio | Durante o foco, abrir um app não liberado leva à tela *"Você não pode fugir da batalha!"*. Telefone, chamadas e emergência sempre liberados |
| Notificações | Contagem contínua (foco, pausa, descanso), alertas de fim de foco/descanso e lembrete diário quando há quests pendentes |
| Configurações | Duração do foco (1–60 min) e do descanso (1–30 min), apps liberados, horário do lembrete e status das permissões |

## Como executar

1. Abra a pasta `11-quest-pomodoro/` no **Android Studio** (Ladybug ou mais recente) e aguarde o Gradle sincronizar.
2. Escolha um emulador ou dispositivo com Android 8.0+ (API 26+) e clique em **Run ▶**.

Pela linha de comando (Git Bash), com o JDK 21 do Android Studio:

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew :app:testDebugUnitTest   # testes unitários
./gradlew :app:installDebug        # instala no emulador/dispositivo conectado
```

## Ativando o bloqueio

O bloqueio usa um **Serviço de Acessibilidade**, que precisa ser ativado uma vez pelo usuário:

1. Na Taverna, toque em ⚔️ numa quest e escolha **Ativar bloqueio** (ou vá em Configurações → Permissões).
2. Nas configurações do Android, abra **Acessibilidade → Escudo Anti-Distração (Quest Pomodoro)** e ative.

> O serviço só observa **qual app** veio para a frente durante o foco. Ele não lê o conteúdo da tela.
> Sem o serviço ativado, a batalha funciona normalmente, mas com o aviso "Bloqueio desativado".

## Permissões

| Permissão | Por quê |
|---|---|
| `POST_NOTIFICATIONS` | Mostrar a contagem, os alertas de fase e o lembrete diário |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | Manter a notificação contínua da sessão |
| `USE_EXACT_ALARM` / `SCHEDULE_EXACT_ALARM` (≤ API 32) | Disparar o fim da fase na hora certa, mesmo com a tela apagada |
| `RECEIVE_BOOT_COMPLETED` | Reagendar a sessão depois de reiniciar o celular |
| `BIND_ACCESSIBILITY_SERVICE` | Detectar o app em primeiro plano para bloquear durante o foco |
| `<queries>` (HOME/LAUNCHER) | Listar os apps instalados para a lista de liberados e identificar o launcher |

## Arquitetura

MVVM com regras de negócio puras em `domain/`, todas testadas na JVM:

```text
app/src/main/java/com/exemplo/questpomodoro/
├── domain/        SessaoMaquina (estados FOCO/PAUSADO/DESCANSO/AGUARDANDO), QuestRegras, Heroi, RegrasBloqueio, Lembrete
├── data/          Room (quests, batalhas, sessão ativa), DataStore (configurações, XP), repositórios
├── service/       Foreground service, alarme de fase, receivers, serviço de acessibilidade
├── notification/  Canais, conteúdo das notificações, lembrete (WorkManager)
└── ui/            Tema RPG, componentes pixel, telas (Taverna, Quest, Batalha, Configurações, Bloqueio)
```

- O tempo restante é sempre `faseTerminaEm − agora`, nunca um contador. Rotação, tela apagada e morte do processo não alteram o timer.
- Todas as operações da sessão passam por um `Mutex`: o alarme e a tela podem sincronizar ao mesmo tempo e o fim da fase é contado uma vez só.

## Créditos

- Fonte **Press Start 2P**, de CodeMan38 ([SIL Open Font License](licenses/OFL-PressStart2P.txt)).
- Paleta **Sweetie 16**, de GrafxKid (CC0).
- Sprites (herói, espada, fogueira, caneca, troféu, escudo) desenhados em código para este projeto.

## Capturas de tela

As capturas ficam em [`screenshots/`](screenshots/).
