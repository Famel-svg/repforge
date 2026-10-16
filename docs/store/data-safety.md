# RepForge - Respostas do Play Console (Data safety, Saúde, Classificação)

Versão do app: 2.0.0 (`com.repforge.app`). Política de privacidade: [URL DA POLÍTICA]. Contato: [EMAIL DE CONTATO].

## 1. Formulário Data safety (Segurança dos dados)

| Pergunta | Resposta | Justificativa |
|---|---|---|
| O app coleta ou compartilha algum tipo de dado de usuário exigido? | **Não** | O app não transmite dados ao desenvolvedor. A biblioteca do Google Play Billing usa a rede apenas para a compra do Pro, processada pelo Google Play; o desenvolvedor não recebe dados de pagamento. |
| Todos os dados do usuário coletados pelo app são criptografados em trânsito? | Não se aplica (a pergunta não aparece quando nada é coletado) | Nenhum dado sai do aparelho. |
| Os usuários podem pedir a exclusão dos dados? | Não se aplica | Não há conta nem dados em poder do desenvolvedor; o usuário apaga localmente ou desinstala. |
| Dados compartilhados com terceiros | **Nenhum** | Não há SDKs de terceiros que coletem dados, anúncios nem análise. |
| Categorias (localização, informações pessoais, saúde e fitness, financeiras, mensagens, fotos, áudio, arquivos, calendário, contatos, atividade no app, navegação, dispositivo/IDs) | **Nenhuma marcada** | Os registros de treino ficam apenas no armazenamento privado do aparelho e nunca são enviados ao desenvolvedor. |
| Link da política de privacidade | [URL DA POLÍTICA] | Obrigatório mesmo sem coleta. |

Observações:

- Dados tratados apenas no aparelho, sem sair dele, não contam como "coletados" para o formulário.
- O backup do sistema Android (conta Google) é operado pelo Google e não pelo app ou desenvolvedor; não é declarado como coleta.
- Backups exportados pelo usuário são JSON legíveis salvos onde o usuário escolhe; o app não os envia a ninguém.
- Compras: a biblioteca do Google Play Billing (SDK do Google) é a única que usa a rede; os dados de compra são tratados pelo Google Play, não pelo desenvolvedor, então não se declara "informações financeiras" como coletadas. Marque "Contém compras no app" no Play Console.
- Marque que o app não usa SDKs que coletam dados. Revise a resposta se qualquer dependência nova for adicionada.

## 2. Declaração de apps de saúde (Health apps declaration)

| Pergunta | Resposta | Justificativa |
|---|---|---|
| Tipo de app de saúde | Atividade física e fitness (registro de treino) | O app registra séries, repetições e cargas. |
| Funcionalidades médicas ou de diagnóstico | **Nenhuma** | Não há conselhos, diagnósticos, tratamento nem alegações médicas. |
| Integração com Health Connect | **Não** | Não usa Health Connect nem sensores de saúde. |
| Coleta de dados de saúde pelo desenvolvedor | **Não** | Nenhum dado de saúde é transmitido ao desenvolvedor. |
| Conteúdo de saúde sensível (condições, medicamentos, saúde mental etc.) | **Não** | Não há esse conteúdo. |

## 3. Questionário de classificação de conteúdo (destaques)

| Tema | Resposta | Justificativa |
|---|---|---|
| Violência | Não | O app não tem violência. |
| Sexualidade e nudez | Não | Sem conteúdo desse tipo. |
| Linguagem ofensiva | Não | Sem conteúdo gerado de terceiros. |
| Drogas, álcool, tabaco | Não | Sem referência. |
| Jogos de azar | Não | Sem apostas. |
| Conteúdo gerado por usuários / interação entre usuários | Não | Sem recursos sociais; o usuário só vê os próprios dados. |
| Compartilhamento de localização | Não | Sem permissão de localização. |
| Compras digitais | **Sim** | Desbloqueio vitalício "RepForge Pro" (`repforge_pro_lifetime`), processado pelo Google Play Billing. |
| Anúncios | Não | O app não exibe anúncios. |
| Categoria do app | Utilitário/Saúde e fitness, não é jogo | Registro de treino. |

Resultado esperado: classificação livre/para todos (por exemplo, Livre no Brasil e Everyone/PEGI 3 em outras regiões), sujeito à conclusão do questionário pelo Play Console.

## 4. Outras declarações relacionadas

- **Público-alvo:** adolescentes e adultos (sugerido 13+ ou 18+), e não direcionado a crianças; evite marcar faixas infantis.
- **Anúncios:** o app não contém anúncios.
- **Exclusão de conta:** não se aplica, não há contas.
- **Permissões:** apenas `com.android.vending.BILLING` (e as de rede mescladas pela biblioteca de Billing); nenhuma permissão sensível.
- **Segurança financeira/outros formulários:** responder "não" para recursos financeiros, VPN, SMS etc.

## 5. Play Billing (já implementado) - conferências

- Adicionar a biblioteca do Google Play Billing normalmente faz o manifesto incluir a permissão `com.android.vending.BILLING`. A permissão de internet pode ser exigida por dependências; verifique o manifesto mesclado do AAB.
- O app passa a ter compras no app: atualize "Contém compras no app" no Play Console e o questionário de classificação (compras digitais: sim).
- Data safety: revise a seção de dados financeiros. Compras processadas pelo Google Play normalmente não exigem declarar dados de compra como coletados pelo desenvolvedor, mas confirme com o texto atual da ajuda do Play Console. Se houver verificação de compra em servidor próprio ou outro SDK, declare conforme os dados realmente enviados.
- Atualize a política de privacidade (seção 9), a descrição da loja ("Pro" e preços) e o campo de alterações.
- Mantenha as respostas atuais de "não coleta" apenas enquanto isso continuar verdadeiro; revise a declaração a cada nova versão que adicionar SDK ou permissão.
