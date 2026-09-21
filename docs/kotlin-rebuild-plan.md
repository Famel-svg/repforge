# RepForge Kotlin rebuild

## Direção de produto

RepForge vira app Android nativo, offline-first e focado em registrar treino com o menor atrito possível. Dados de treino ficam no aparelho; rede serve apenas para enriquecer busca de exercícios.

## Melhorias incorporadas

- Jetpack Compose + Material 3 com cores dinâmicas e suporte a dark mode.
- Navegação inferior orientada a tarefas: Treino, Evolução e Ajustes.
- Home com próxima ação clara, resumo de volume semanal e estado vazio acionável.
- Room como fonte local reativa; UI observa `Flow` e continua útil sem internet.
- Domínio separado da persistência para facilitar testes e evolução.
- Base pronta para adaptação a tablets com Material 3 Adaptive.

## Próximos incrementos

1. Detalhe da ficha: adicionar, ordenar e remover exercícios.
2. Modo treino: registrar séries rapidamente, timer de descanso e edição inline.
3. Evolução: volume, carga máxima, PRs e gráficos por exercício.
4. Busca WorkoutX via proxy, com cache local e estados offline explícitos.
5. Exportação/importação JSON com validação, transação e backup compartilhável.
6. Testes unitários de domínio, Room e testes Compose de acessibilidade.

## Referências de design

- [Offline-first Android architecture](https://developer.android.com/topic/architecture/data-layer/offline-first)
- [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Compose Material 3 Adaptive](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive)
