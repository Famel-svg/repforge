# RepForge - Checklist de lançamento no Google Play

Pacote: `com.repforge.app`. Mercado principal: Brasil (pt-BR). Requisitos pesquisados em out/2026; confirme sempre na Central de Ajuda do Play Console antes de cada etapa.

## 1. Conta de desenvolvedor e verificação

- [ ] Criar a conta de desenvolvedor do Google Play (conta pessoal) e pagar a taxa de inscrição.
- [ ] Concluir a verificação de identidade do Google (documento, endereço, telefone, e-mail de contato público).
- [ ] Concluir a verificação de desenvolvedor Android, que é exigida no Brasil desde 30/09/2026. Registre o pacote `com.repforge.app` na verificação.
- [ ] Ativar verificação em duas etapas na conta Google do desenvolvedor.
- [ ] Definir e-mail de contato público: [EMAIL DE CONTATO].

## 2. Chave de assinatura e Play App Signing

- [ ] Gerar o keystore de upload (`keytool -genkeypair -v -keystore upload-keystore.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`).
- [ ] Guardar o keystore e as senhas em local seguro com cópia de segurança fora do repositório. Nunca versionar o arquivo nem as senhas.
- [ ] Configurar a assinatura de release no Gradle lendo as credenciais de variáveis de ambiente ou de um arquivo local ignorado pelo Git.
- [ ] Aceitar o Play App Signing ao enviar o primeiro AAB (o Google guarda a chave de assinatura final; você usa a chave de upload).

## 3. Build do release

- [ ] Confirmar `targetSdk` 36, `minSdk` 26, `applicationId` `com.repforge.app` e `versionName` 2.0.0 com `versionCode` único e crescente.
- [ ] Confirmar que o manifesto mesclado não tem permissões (`INTERNET` ausente) com a ferramenta de análise do AAB/APK.
- [ ] Gerar o AAB: `./gradlew :app:bundleRelease`.
- [ ] Verificar o AAB assinado e testar o APK gerado a partir dele em aparelho real (bundletool `build-apks` + `install-apks`).
- [ ] Confirmar com minify/shrink de release que nada quebrou (fluxos: criar ficha, registrar série, timer, progresso, exportar e importar JSON, tema).
- [ ] Guardar o arquivo `mapping.txt` (se houver ofuscação) para cada versão enviada.

## 4. Verificação de 16 KB (page size)

- [ ] Alinhar bibliotecas nativas (`.so`) a 16 KB, se houver; use AGP e NDK recentes.
- [ ] Rodar a verificação de alinhamento (por exemplo `zipalign -c -P 16 -v 4 app.apk` e `check_elf_alignment.sh`) e conferir os avisos do Play Console no envio.
- [ ] Testar em emulador Android com imagem de 16 KB de página.

## 5. Ativos da loja

- [ ] Ícone do app 512x512 PNG (32 bits, até 1 MB).
- [ ] Gráfico de recursos 1024x500 (PNG ou JPEG, sem transparência).
- [ ] Refazer as capturas em aparelho real: as de `docs/store/screenshots/pt-BR/` são anteriores à navegação em 4 abas. Telas sugeridas (8): Início com o cartão "Treino de hoje", Plano, sessão de treino, resumo ao finalizar, Evolução com mapa de atividade, mapa do corpo, Biblioteca e Configurações > Aparência.
- [ ] Capturas de tela de celular: ao menos 2, ideal 8, proporção 16:9 ou 9:16, lados entre 320 px e 3840 px. Use as legendas de `play-listing.md`.
- [ ] Textos pt-BR e en-US de `play-listing.md` (título até 30, descrição curta até 80, completa até 4000).
- [ ] Categoria Saúde e fitness; e-mail de contato; URL da política: [URL DA POLÍTICA].

## 6. Política de privacidade

- [ ] Preencher [NOME DO DESENVOLVEDOR], [EMAIL DE CONTATO] e [DATA] em `privacy-policy.md`.
- [ ] Publicar o texto em uma URL pública e estável (GitHub Pages, Google Sites ou site próprio), sem login e sem PDF.
- [ ] Registrar a URL no Play Console e dentro do app, se houver tela de sobre.

## 7. Formulários do Play Console

- [ ] Conteúdo do app > Política de privacidade (URL).
- [ ] Segurança dos dados (Data safety), conforme `data-safety.md`.
- [ ] Declaração de apps de saúde (fitness, sem recursos médicos), conforme `data-safety.md`.
- [ ] Classificação de conteúdo (questionário IARC).
- [ ] Público-alvo e conteúdo; anúncios: o app não tem anúncios.
- [ ] Declaração de acesso ao app: acesso sem login, sem restrição.
- [ ] Países e regiões de distribuição; preço: gratuito.

## 8. Teste interno

- [ ] Criar a faixa de teste interno e adicionar até 100 testadores (e-mails Google).
- [ ] Enviar o AAB, instalar pelo link de teste e validar em aparelhos variados (Android 8 até o mais novo, telas pequenas e grandes).
- [ ] Corrigir bugs encontrados e incrementar `versionCode` a cada novo envio.

## 9. Teste fechado (12 testadores, 14 dias)

Exigência para contas pessoais novas: teste fechado com pelo menos 12 testadores ativos (com opt-in) por 14 dias contínuos antes de solicitar produção.

- [ ] Criar a faixa de teste fechado e a lista de testadores (grupo do Google Groups ou lista de e-mails).
- [ ] Recrutar 15 a 20 testadores para ter folga caso alguém saia; todos precisam aceitar o convite (opt-in) e manter a participação.
- [ ] Enviar a versão 2.0.0 e registrar a [DATA] de início da contagem de 14 dias.
- [ ] Acompanhar no Play Console se o número de testadores optados continua em 12 ou mais todos os dias.
- [ ] Coletar feedback e corrigir problemas; novas versões durante o teste não reiniciam o prazo, mas confirme a regra atual.

Dicas para recrutar testadores no Brasil:

- [ ] Amigos, familiares e colegas de academia: peça que instalem pelo link e abram o app em dias diferentes.
- [ ] Grupos de WhatsApp e Telegram de treino, musculação e academia, explicando que é um app gratuito sem anúncios e sem coleta de dados.
- [ ] Comunidades de Reddit (por exemplo r/brasil, r/Fitness em português), Discord de programação e fitness e grupos de Facebook de testadores de apps.
- [ ] Comunidades de desenvolvedores Android no Brasil (grupos de testes mútuos, onde você testa o app deles e eles o seu).
- [ ] Redes sociais pessoais e LinkedIn, com mensagem curta, link de convite e passo a passo de opt-in.
- [ ] Mande lembretes semanais e um formulário simples de feedback; testadores que desistem precisam ser substituídos antes do fim dos 14 dias.
- [ ] Não use serviços de pagamento por instalação nem contas falsas; isso viola as políticas do Google.

## 10. Solicitar acesso à produção

- [ ] Após os 14 dias com 12 ou mais testadores, abrir "Solicitar acesso à produção" no Play Console.
- [ ] Responder o questionário (como recrutou testadores, feedback recebido, mudanças feitas, preparo para produção).
- [ ] Aguardar a análise do Google (normalmente alguns dias).

## 11. Produção e lançamento gradual

- [ ] Criar a versão de produção com o AAB final e notas de versão (`play-listing.md`).
- [ ] Enviar para revisão e corrigir qualquer rejeição de política.
- [ ] Iniciar com lançamento gradual (por exemplo 10% a 20%) e aumentar para 100% se não houver problemas.
- [ ] Conferir a página pública da loja (textos, imagens, link da política).

## 12. Pós-lançamento

- [ ] Monitorar o Android vitals (travamentos e ANRs) e as avaliações no Play Console; o app não tem relatório de falhas próprio.
- [ ] Responder avaliações e e-mails de [EMAIL DE CONTATO].
- [ ] Planejar atualizações regulares e manter o `targetSdk` atualizado nas exigências anuais do Google.
- [ ] Antes de lançar a versão com Play Billing, atualizar política, Data safety, classificação e descrição (ver `data-safety.md`, seção 5).
- [ ] Manter backup do keystore e acompanhar o prazo de renovação de verificações da conta.
