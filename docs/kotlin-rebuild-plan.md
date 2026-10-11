# RepForge Kotlin rebuild

## Direção de produto

RepForge vira app Android nativo, offline-first e focado em registrar treino com o menor atrito possível. Dados de treino ficam no aparelho; rede serve apenas para enriquecer busca de exercícios.

## Melhorias incorporadas

- Jetpack Compose + Material 3 com cores dinâmicas e suporte a dark mode.
- Navigation Compose: abas Treino, Evolução e Ajustes; ficha e busca em tela cheia com voltar do sistema.
- Home com próxima ação clara, resumo de volume dos últimos 7 dias e estado vazio acionável.
- Room como fonte local reativa; UI observa `Flow` e continua útil sem internet.
- Domínio separado da persistência para facilitar testes e evolução.
- Confirmação visual e háptica de série registrada; timer de descanso inicia sozinho.
- Remoção de exercício com confirmação e cascata do histórico no Room.
- Edição de nome e grupo muscular sem apagar o histórico.
- Duplicação de ficha, PR de carga e histórico datado por exercício.
- Ações e campos reorganizados para escala de fonte ampliada; teclado numérico e vírgula decimal.
- Backup JSON com validação, importação aditiva em transação e leitura do formato Expo 1.0.x.
- Busca WorkoutX por nome, parte do corpo ou equipamento.
- Build release com R8, schema Room exportado e versionCode acima do app Expo.

## Melhorias futuras

1. GIFs dos exercícios (exige download autenticado pelo proxy e cache local).
2. Tradução de nomes de exercício e termos de busca para português.
3. Reordenar exercícios por arrastar e soltar.
4. Cache local de resultados WorkoutX e estado offline explícito na busca.
5. Gráficos históricos por exercício.
6. Testes Compose de acessibilidade e testes instrumentados de Room.
7. Adaptação a tablets com Material 3 Adaptive.

## Referências de design

- [Offline-first Android architecture](https://developer.android.com/topic/architecture/data-layer/offline-first)
- [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Compose Material 3 Adaptive](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive)
