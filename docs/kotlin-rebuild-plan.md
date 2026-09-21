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
- Confirmação visual de série registrada com Snackbar.
- Remoção de exercício com confirmação e cascata do histórico no Room.
- Edição de nome e grupo muscular sem apagar o histórico.
- Ações e campos reorganizados para escala de fonte ampliada.
- Backup JSON com validação, transação e seletores de arquivo Android.
- APK debug instalado e validado em emulador Android real.

## Melhorias futuras, fora do núcleo atual

1. Reordenar exercícios por arrastar e soltar.
2. Cache local de resultados WorkoutX e estado offline explícito na busca.
3. Gráficos históricos por exercício.
4. Testes Compose de acessibilidade e testes instrumentados de Room.

## Referências de design

- [Offline-first Android architecture](https://developer.android.com/topic/architecture/data-layer/offline-first)
- [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Compose Material 3 Adaptive](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive)
