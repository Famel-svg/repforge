# RepForge

Aplicativo Android local-first para organizar fichas de treino, registrar cargas
e acompanhar histórico de séries e repetições.

Construído com Expo, React Native, TypeScript e SQLite. Dados permanecem no
dispositivo e podem ser exportados ou importados em JSON.

## Recursos

- Criação, exclusão e duplicação de fichas de treino
- Busca de exercícios pela API WorkoutX
- Filtros por nome, parte do corpo e equipamento
- GIF, nome traduzido e dados do exercício salvos localmente
- Registro rápido de múltiplas séries, repetições e carga em quilogramas
- Histórico ordenado com último registro, volume, PR de carga e 1RM estimado
- Exibição automática da carga atual na ficha
- Dashboard com volume semanal, totais e barras dos últimos 7 dias
- Timer de descanso ajustável que inicia após salvar registro
- Botão para copiar o último registro do exercício
- Banco SQLite com exclusão em cascata
- Exportação e importação de backup JSON
- Importação transacional com rollback em caso de erro
- Interface escura em português
- Perfil EAS configurado para gerar APK

## Tecnologias

| Tecnologia | Uso |
|---|---|
| Expo SDK 56 | Plataforma e build Android |
| React Native | Interface mobile |
| TypeScript | Tipagem estática |
| Expo SQLite | Persistência local |
| React Navigation | Navegação entre telas |
| Axios | Integração HTTP |
| WorkoutX API | Catálogo de exercícios |
| Vitest | Testes unitários |
| ESLint | Qualidade de código |

## Fluxo do aplicativo

```text
Fichas
  └── Exercícios da ficha
        ├── Histórico e novo registro
        └── Busca de exercícios na WorkoutX
```

1. Usuário cria uma ficha.
2. Busca um exercício na WorkoutX.
3. Nome e URL do GIF são persistidos no SQLite.
4. Registros de carga ficam vinculados ao exercício.
5. Backup pode ser compartilhado ou restaurado posteriormente.

## Estrutura

```text
repforge/
├── App.tsx                    navegação e inicialização do SQLite
├── app.json                   configuração Expo e Android
├── eas.json                   perfis de build EAS
├── src/
│   ├── components/            componentes visuais
│   ├── db/                    schema e repositórios SQLite
│   ├── navigation/            tipos das rotas
│   ├── screens/               telas do aplicativo
│   ├── services/              cliente WorkoutX
│   ├── types/                 contratos do domínio
│   └── utils/                 backup, importação, timer, cálculos e formatação
└── tests/                     testes de banco, backup e API
```

## Requisitos

- Node.js 22.13 ou superior
- npm
- Expo Go ou dispositivo/emulador Android
- Chave da [WorkoutX](https://workoutxapp.com/)
- Conta Expo somente para builds EAS

Node 22 LTS é recomendado.

## Instalação

```bash
git clone https://github.com/Famel-svg/repforge.git
cd repforge
npm install
```

Crie `.env` a partir do modelo:

### Windows PowerShell

```powershell
Copy-Item .env.example .env
```

### Linux/macOS

```bash
cp .env.example .env
```

Configure a chave:

```dotenv
EXPO_PUBLIC_WORKOUTX_KEY=wx_sua_chave_real
```

`EXPO_PUBLIC_*` é incorporada ao aplicativo compilado. Não trate essa variável
como segredo de servidor. Restrinja uso e cota no provedor quando disponível.

## Execução

```bash
npm start
```

Outros comandos:

```bash
npm run android
npm run ios
npm run web
```

O foco atual é Android. iOS e web não fazem parte da validação do MVP.

## Banco local

Arquivo: `repforge.db`

Tabelas:

- `sheets`: fichas de treino
- `exercises`: exercícios vinculados à ficha
- `entries`: histórico de séries, repetições e carga

O schema usa `PRAGMA foreign_keys = ON`, exclusão em cascata e
`PRAGMA user_version = 1`.

## Backup

Exportação gera:

```text
repforge-backup-YYYY-MM-DD.json
```

Importação segue estratégia append:

- dados existentes são preservados;
- arquivo completo é validado antes da escrita;
- todas as inserções executam em uma transação exclusiva;
- qualquer falha causa rollback completo.

## Qualidade

```bash
npm run lint
npm run typecheck
npm test
npx expo-doctor
```

Cobertura funcional atual:

- schema e exclusão em cascata;
- ordenação de exercícios;
- seleção da entrada mais recente;
- validação e serialização do backup;
- importação válida e inválida;
- rollback transacional;
- normalização da resposta WorkoutX;
- erro de chave ausente;
- cálculos de volume, PR e 1RM;
- agregação do dashboard;
- timer de descanso.

## Gerar APK

Instale e autentique o EAS CLI:

```bash
npm install --global eas-cli
eas login
```

Configure `EXPO_PUBLIC_WORKOUTX_KEY` no ambiente `preview` do EAS e execute:

```bash
eas build --platform android --profile preview
```

O perfil `preview` produz APK instalável. `production` fica reservado para
distribuição futura em AAB.

## Segurança

- `.env`, APKs, logs Expo e dependências estão ignorados pelo Git.
- Backup contém dados de treino em texto legível; compartilhe com cuidado.
- Aplicativo não possui autenticação ou sincronização em nuvem.
- Chave WorkoutX não deve ser adicionada diretamente ao código.

## Licença

Distribuído sob licença MIT. Consulte [LICENSE](LICENSE).


