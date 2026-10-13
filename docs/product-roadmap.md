# RepForge — roadmap de produto

Atualizado em 13/10/2026.

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

1. **Offline e sem conta.** O app não pede nenhuma permissão Android; "nenhum
   dado coletado" na ficha de segurança da Play Store é argumento de venda.
2. **Sem limite no essencial.** Fichas, exercícios e histórico ilimitados no
   plano grátis — a principal reclamação nos concorrentes é paywall no básico.
3. **Português de academia brasileira.** Catálogo próprio com os nomes usados
   aqui, kg por padrão, preço em reais.
4. **Dados são do usuário.** Backup JSON legível, importação que nunca apaga.
5. **Registro em um toque.** Série pré-preenchida, timer automático.

## Modelo de negócio (proposta)

| | Grátis | Pro |
|---|---|---|
| Fichas, exercícios, histórico | ilimitado | ilimitado |
| Catálogo, modelos básicos, timer, PR, 1RM | sim | sim |
| Backup manual (exportar/importar) | sim | sim |
| Evolução: sequência, volume 7 dias, treinos recentes | sim | sim |
| Tendência de 1RM além de 30 dias, volume por grupo muscular | — | sim |
| Programas avançados e periodização | — | sim |
| Calculadora de anilhas e aquecimento | — | sim |
| Backup automático agendado, widget, temas | — | sim |

Preço inicial sugerido (estimativa, validar após lançamento): vitalício
R$ 59,90 como oferta principal; anual R$ 29,90; mensal R$ 6,90. Sem anúncios
em nenhum plano.

Regra: nada que hoje é grátis passa a ser pago.

## Marcos

### M1 — Base vendável (em andamento)

- [x] Backup aditivo e migração do app Expo
- [x] Navegação real, teclado numérico, timer automático, háptico
- [x] Build release com R8, versionCode acima do Expo, schema Room exportado
- [x] Identidade visual própria (tema brasa/aço, splash)
- [x] Aba Evolução: sequência, gráfico de volume, tendência de 1RM, histórico
- [x] CI no GitHub Actions
- [ ] Catálogo de exercícios próprio e offline; app sem permissão de internet
- [ ] Registro de série em um toque, séries do dia, desfazer, reordenar
- [ ] Modelos de treino prontos
- [ ] Textos da loja, política de privacidade, checklist de publicação

### M2 — Teste fechado (exigência da Play Store)

- [ ] Conta de desenvolvedor verificada; chave de assinatura e Play App Signing
- [ ] Strings em recursos, inglês como segundo idioma
- [ ] Testes instrumentados (Room, fluxo principal) e teste de migração
- [ ] Onboarding de primeira execução
- [ ] Acessibilidade revisada com TalkBack e fonte grande
- [ ] 12 testadores por 14 dias contínuos, com canal de feedback
- [ ] Capturas de tela e gráfico de destaque

### M3 — Lançamento com Pro

- [ ] Play Billing 9.x: vitalício + assinatura; restauração de compra
- [ ] Recursos Pro da tabela acima
- [ ] Sessão de treino explícita (início/fim, duração) — exige migração Room
- [ ] Peso corporal e medidas
- [ ] Lançamento gradual (staged rollout) e resposta a avaliações

### M4 — Crescimento

- [ ] Widget e notificação persistente do timer
- [ ] Health Connect (opcional, por consentimento)
- [ ] Imagens ou animações de exercícios com licença própria
- [ ] Backup automático no Drive via Storage Access Framework
- [ ] Wear OS

## Riscos

- **Teste fechado**: produção só libera após 12 testadores por 14 dias; é o
  caminho crítico do cronograma.
- **Migração de quem usa o Expo**: atualização por cima exige a chave do EAS;
  sem ela o caminho é exportar, reinstalar e importar.
- **Catálogo sem imagens**: concorrentes mostram demonstrações; mitigado por
  nomes corretos em português e exercícios personalizados.
- **Preço**: estimado sem benchmark brasileiro; testar depois do lançamento.
