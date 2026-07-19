# RepForge

![RepForge icon](assets/icon.png)

RepForge e um app Android local-first para montar fichas de treino, buscar
exercicios, registrar cargas e acompanhar evolucao sem depender de conta ou
nuvem.

O app usa Expo, React Native, TypeScript e SQLite. Seus dados ficam no aparelho
e podem ser exportados/importados em JSON pela tela `Config`.

## O que ele faz

- Cria, duplica e remove fichas de treino.
- Busca exercicios na WorkoutX com filtros de nome, parte do corpo e equipamento.
- Salva nome, GIF e dados do exercicio no banco local.
- Registra series, repeticoes e carga em kg.
- Mostra historico, ultimo registro, volume, PR de carga e 1RM estimado.
- Mostra dashboard com volume semanal, totais e barras dos ultimos 7 dias.
- Inclui timer de descanso ajustavel.
- Exporta e importa backup JSON.
- Usa interface escura em portugues, pensada para celular.

## WorkoutX API

RepForge nao embute chave WorkoutX no APK.

Motivo: APK e codigo cliente. Qualquer chave colocada em JavaScript, `.env`,
`EXPO_PUBLIC_*`, `app.json`, asset ou recurso nativo pode ser extraida por
alguem que descompile o APK ou inspecione as requisicoes.

Opcoes seguras:

- **Uso pessoal ou beta:** abra `Config` no app e cole sua propria chave
  WorkoutX (`wx_...`). Ela fica salva apenas no SQLite local do aparelho.
- **Distribuicao publica usando sua chave:** crie um backend proxy. O app chama
  seu servidor, e o servidor chama a WorkoutX com `X-WorkoutX-Key`. Assim o APK
  recebe apenas a URL publica do proxy, nunca a chave.

O build publico atual usa a primeira opcao.

## Instalar para desenvolvimento

Requisitos:

- Node.js 22.13 ou superior
- npm
- Expo Go, emulador Android ou aparelho Android
- Conta Expo apenas para gerar APK via EAS

```bash
git clone https://github.com/Famel-svg/repforge.git
cd repforge
npm install
```

## Rodar no computador

No VS Code, use `Terminal > Run Task`:

- `Preview: Web`
- `Preview: Expo Go`

Ou rode direto:

```bash
npm run web
npm start
```

O foco do projeto e Android. Web existe para preview rapido.

## Rodar no Android

```bash
npm run android
```

Depois abra `Config`, cole sua chave WorkoutX e salve.

## Gerar APK

```bash
npm run lint
npm run typecheck
npm test
npx eas-cli build --platform android --profile preview
```

O perfil `preview` gera APK instalavel. O perfil `production` fica reservado
para AAB/loja.

## Backup

Exportacao gera arquivo no formato:

```text
repforge-backup-YYYY-MM-DD.json
```

Importacao:

- valida o arquivo antes de escrever;
- adiciona dados sem apagar banco atual;
- roda em transacao exclusiva;
- faz rollback completo se algo falhar.

Backup contem dados de treino em texto legivel. Compartilhe com cuidado.

## Banco local

Banco: `repforge.db`

Tabelas:

- `sheets`: fichas de treino
- `exercises`: exercicios da ficha
- `entries`: historico de carga
- `app_settings`: configuracoes locais, incluindo chave WorkoutX do usuario

O schema ativa `PRAGMA foreign_keys = ON`, usa exclusao em cascata e
`PRAGMA user_version = 2`.

## Estrutura

```text
repforge/
├── App.tsx
├── app.json
├── eas.json
├── assets/
├── src/
│   ├── components/
│   ├── db/
│   ├── navigation/
│   ├── screens/
│   ├── services/
│   ├── types/
│   └── utils/
└── tests/
```

## Qualidade

```bash
npm run lint
npm run typecheck
npm test
```

Testes cobrem schema SQLite, backup/importacao, rollback, WorkoutX,
normalizacao de dados, dashboard, timer e calculos de treino.

## Release

1. Atualize `package.json`, `package-lock.json`, `app.json` e `versionCode`.
2. Rode lint, typecheck e testes.
3. Gere APK com EAS preview.
4. Baixe o APK.
5. Crie tag e release no GitHub.

## Licenca

MIT. Veja [LICENSE](LICENSE).
