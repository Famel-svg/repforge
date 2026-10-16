# RepForge — roadmap de produto

Atualizado em 16/10/2026. Versão atual: 2.0.0 (versionCode 10).

## Decisão de plataforma

Kotlin nativo (Compose + Room) é a base do produto comercial. O app Expo 1.0.x
fica congelado na branch `main` apenas como referência.

Motivos:

- O alvo é só Android; multiplataforma não paga o custo do Expo/EAS.
- APK release de ~1,7 MB contra ~90 MB do build Expo.
- Sem dependência de serviço de build de terceiros nem de runtime JS.
- Acesso direto a Play Billing, widgets, Health Connect e notificações.

## Posicionamento

Diário de musculação rápido, em português, que funciona 100% offline, sem
conta, sem anúncio e sem coletar dado nenhum.

Diferenciais que o produto sustenta:

1. **Offline e sem conta.** O único uso de rede é o Google Play Billing; o
   desenvolvedor não coleta dado nenhum, e isso na ficha de segurança da Play
   Store é argumento de venda.
2. **Sem limite no essencial.** Fichas, exercícios e histórico ilimitados no
   plano grátis — a principal reclamação nos concorrentes é paywall no básico.
3. **Português de academia brasileira.** Catálogo próprio com os nomes usados
   aqui, kg por padrão, preço em reais.
4. **Dados são do usuário.** Backup JSON legível, importação que nunca apaga.
5. **Registro em um toque.** Série pré-preenchida, timer automático.

## Modelo de negócio

Entregue: **RepForge Pro vitalício** (compra única, Play Billing,
`repforge_pro_lifetime`). Assinaturas ficaram fora do escopo.

| | Grátis | Pro |
|---|---|---|
| Fichas, exercícios, histórico, registro, timer, supersets | ilimitado | ilimitado |
| Catálogo, modelos, PR, 1RM, peso corporal | sim | sim |
| Backup manual (exportar/importar JSON, CSV Strong/Hevy) | sim | sim |
| Evolução: sequência, volume 7 dias, séries por músculo, treinos recentes | sim | sim |
| Plano semanal, sessões, regras de progressão, tipos de exercício, Biblioteca, widget, temas | sim | sim |
| Mapa de atividade de 52 semanas e mapa corporal (curto) | sim | sim |
| 1RM em 90 dias e todo o histórico | — | sim |
| Comparação de volume por músculo em 4 semanas | — | sim |
| Mapa corporal por mês e todo o histórico | — | sim |
| Sugestões de progressão de carga | — | sim |
| Calculadora de anilhas | — | sim |

Preço: definir no Play Console (referência inicial R$ 59,90, a validar após o
lançamento). Sem anúncios.

Regra: nada que hoje é grátis passa a ser pago.

## Marcos

### M1 — Base vendável (concluído)

- [x] Backup aditivo e migração do app Expo
- [x] Navegação real, teclado numérico, timer automático, háptico
- [x] Build release com R8, versionCode acima do Expo, schema Room exportado
- [x] Identidade visual própria (tema brasa/aço, splash)
- [x] Aba Evolução: sequência, gráfico de volume, 1RM por exercício, histórico
- [x] CI no GitHub Actions
- [x] Catálogo offline de 264 exercícios; busca WorkoutX removida do app
- [x] Registro de série em um toque, séries do dia, desfazer, reordenar, supersets
- [x] Modelos de treino prontos (Full body, ABC, Upper/Lower)
- [x] Textos da loja, política de privacidade, checklist de publicação

### M2 — Teste fechado (em andamento)

- [x] Strings em recursos, inglês e pt-BR com idioma por app
- [x] Testes instrumentados (Room, migração) e schemas até a v5
- [x] Onboarding de primeira execução
- [x] Capturas de tela (pt-BR) para a loja
- [ ] Conta de desenvolvedor verificada; chave de assinatura e Play App Signing
- [ ] Acessibilidade revisada com TalkBack e fonte grande
- [ ] 12 testadores por 14 dias contínuos, com canal de feedback

### M3 — Lançamento com Pro

- [x] Play Billing: Pro vitalício com restauração de compra
- [x] Recursos Pro: 1RM 90 dias/tudo, comparação de 4 semanas por músculo, sugestão de progressão
- [x] Peso corporal
- [x] Tipos de série, RPE opcional, notas por exercício, importação Strong/Hevy
- [ ] Criar o produto `repforge_pro_lifetime` no Play Console e testar a compra
- [ ] Lançamento gradual (staged rollout) e resposta a avaliações

### M4 — Crescimento

Entregue:

- [x] Navegação em 4 abas (Treino, Plano, Evolução, Biblioteca); configurações pela engrenagem
- [x] Plano semanal por dia, reagendamento pontual e cartão "Treino de hoje"
- [x] Sessão de treino explícita (cronômetro, finalizar/descartar, resumo final) e peso corporal opcional ao iniciar
- [x] Tipos de exercício: repetições, tempo, peso corporal, por lado e cardio
- [x] Regras de progressão por ficha com ajuste por exercício (nenhuma, linear, dupla, tempo, deload)
- [x] Mapa de atividade de 52 semanas e mapa corporal frente/costas com músculos não treinados
- [x] Aba Biblioteca
- [x] Tema sistema/claro/escuro, 8 cores de destaque e sons
- [x] Notificação do timer de descanso
- [x] Calculadora de anilhas (Pro)
- [x] Widget da tela inicial
- [x] Backup com plano, sessões e regras; Room v5

Pendente:

- [ ] Imagens ou animações de exercícios com licença própria
- [ ] Mais idiomas
- [ ] Compartilhar plano como arquivo/PDF
- [ ] Importação FitNotes e Apple Health
- [ ] Health Connect (opcional, por consentimento)
- [ ] Backup automático no Drive
- [ ] Wear OS

## Riscos

- **Teste fechado**: produção só libera após 12 testadores por 14 dias; é o
  caminho crítico do cronograma.
- **Produto Pro**: `repforge_pro_lifetime` precisa ser criado e ativado no Play
  Console (e testado com conta de teste) antes de o Pro funcionar em produção.
- **Migração de quem usa o Expo**: atualização por cima exige a chave do EAS;
  sem ela o caminho é exportar, reinstalar e importar.
- **Catálogo sem imagens**: concorrentes mostram demonstrações; mitigado por
  nomes corretos em português e exercícios personalizados (imagens com licença
  própria ficam para o M4).
- **Preço**: estimado sem benchmark brasileiro; testar depois do lançamento.
