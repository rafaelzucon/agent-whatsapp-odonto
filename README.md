# Agente WhatsApp Odonto

Agente de atendimento por WhatsApp para consultório odontológico. Ele conversa com o paciente pelo número oficial do consultório e:

1. **identifica** o paciente pelo RG e pela UF do órgão emissor;
2. **cadastra** o paciente novo (com consentimento LGPD) e **abre o prontuário** (ex.: `PR-000123`);
3. pergunta a **queixa**, classifica no **catálogo de 75 problemas odontológicos** (com CID-10) e registra no prontuário;
4. pergunta **data e horário** desejados e **agenda**; se o horário estiver ocupado ou indisponível, **sugere o livre mais próximo**;
5. **confirma** ao paciente e **avisa o consultório** pelo WhatsApp (nome, data/hora, problema, prioridade e prontuário).

O fluxo é um **workflow agêntico com etapas fixas** (máquina de estados da conversa), previsível e auditável, e não um agente autônomo. Todos os textos enviados são **fixos e configuráveis**. A IA (Claude) só faz duas coisas: **classificar a queixa** no catálogo e **interpretar datas e horários** em linguagem natural ("amanhã à tarde", "sexta às 10h"). **Nenhuma orientação clínica é gerada por IA.**

> Diagramas completos (contexto, camadas, componentes, máquina de estados, sequências, classes, dados e implantação): [`docs/diagramas/`](docs/diagramas/README.md).

## Sumário

- [Exemplo de conversa](#exemplo-de-conversa)
- [Arquitetura](#arquitetura)
- [Stack e versões](#stack-e-versões)
- [Pré-requisitos](#pré-requisitos)
- [Configurando o WhatsApp na Meta](#configurando-o-whatsapp-na-meta)
- [Instalação e operação](#instalação-e-operação)
- [Implantação na AWS Lambda](#implantação-na-aws-lambda)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Personalizando os textos](#personalizando-os-textos)
- [API administrativa](#api-administrativa)
- [Desenvolvimento e testes](#desenvolvimento-e-testes)
- [Segurança e LGPD](#segurança-e-lgpd)
- [Catálogo de problemas odontológicos](#catálogo-de-problemas-odontológicos)
- [Decisões e desvios em relação ao pedido](#decisões-e-desvios-em-relação-ao-pedido)
- [Limitações conhecidas](#limitações-conhecidas)
- [Solução de problemas](#solução-de-problemas)

## Exemplo de conversa

```text
Paciente: Oi
Agente:   Olá! Aqui é o atendimento automático do Consultório Odontológico Sorriso. 🦷
          Para começar, informe o número do seu RG e a UF do órgão emissor (exemplo: 12.345.678-9 SP).
Paciente: 12.345.678-9 sp
Agente:   Não encontrei cadastro com esse RG, então vamos fazer o seu agora. É rapidinho!
          Qual é o seu nome completo?
Paciente: Maria Aparecida da Silva
Agente:   Qual é a sua data de nascimento? (exemplo: 25/03/1990)
Paciente: 25/03/1990
Agente:   Para cadastrar e manter o prontuário, o Consultório Odontológico Sorriso vai guardar seus dados
          pessoais e de saúde com segurança [...] conforme a LGPD. Você autoriza? Responda SIM ou NÃO.
Paciente: sim
Agente:   Cadastro concluído, Maria! O número do seu prontuário é PR-000123.
          Agora conte, com suas palavras, o que está sentindo ou qual é o motivo da consulta.
Paciente: meu dente dói muito quando tomo coisa gelada
Agente:   Anotado! Qual dia e horário você prefere para a consulta? (exemplos: amanhã às 9h, sexta à tarde, 20/11 às 14h)
Paciente: sexta às 10h
Agente:   Esse horário não está disponível. O horário livre mais próximo é sexta-feira, 20/11/2026 às 10:30.
          Posso agendar? Responda SIM, ou diga outro dia e horário.
Paciente: sim
Agente:   Consulta agendada, Maria! ✅ Data: sexta-feira, 20/11/2026 às 10:30. Endereço: Rua Exemplo, 123 - Centro - São Paulo/SP.
          → o consultório recebe no WhatsApp: paciente, data e horário, problema, prioridade e prontuário.
```

Pacientes já cadastrados pulam o cadastro. Quem já tem consulta futura é avisado e pode marcar outra. Queixas de **emergência** (sangramento que não para, inchaço que dificulta respirar ou engolir, trauma, etc.) geram alerta imediato ao consultório e oferecem o primeiro horário livre. Áudios são transcritos (Whisper, opcional). Sem transcrição, o agente pede que o paciente escreva. Depois de algumas respostas inválidas seguidas, a conversa é encaminhada para atendimento humano e a equipe é avisada.

## Arquitetura

**Clean Architecture** com a regra de dependência apontando sempre para dentro, verificada por testes ArchUnit (`ArquiteturaTest`):

```text
infraestrutura   (Spring Boot, composição dos casos de uso, propriedades)
   → adaptador   (entrada: webhook, API admin, ouvintes, rotinas | saída: JPA, WhatsApp, Claude, Whisper, e-mail)
      → aplicacao  (casos de uso *UseCase/*Service, portas de entrada e de saída)
         → dominio   (Java 21 puro: Paciente, Prontuario, Consulta, Conversa, Agenda...)
```

| Pacote (`br.com.consultorio.odonto`) | Responsabilidade |
|---|---|
| `dominio.modelo` | Modelo rico: `Paciente`, `Prontuario`, `QueixaRelatada`, `ProblemaOdontologico`, `Consulta`, `Conversa` (máquina de estados: `registrarResposta(...)` devolve uma `AcaoConversa`), objetos de valor (`DocumentoRg`, `WaId`, `Horario`, `JanelaAtendimento`, `Prioridade`). |
| `dominio.servico` | `Agenda` (horário livre mais próximo), `ClassificadorPorPalavraChave`, `DetectorSinaisAlarme`, `InterpretadorDataHoraDeterministico`, `ValidadorDados`. |
| `aplicacao` | Casos de uso (`ConduzirConversa`, `IdentificarPaciente`, `CadastrarPaciente`, `RegistrarQueixa`, `AgendarConsulta`, `NotificarConsultorio`, `TranscreverAudio`, `GerenciarAgenda`...) e as portas que eles usam. Não depende de Spring. |
| `adaptador.entrada` | `WebhookController` (verificação e assinatura da Meta), `OuvinteEventos` (assíncrono, trava por conversa), `TarefasAgendadas`, controladores da API administrativa. |
| `adaptador.saida` | Persistência JPA com criptografia, `WhatsAppCloudApiAdapter`, adaptadores Claude e Whisper (Spring AI), canais de notificação (WhatsApp, e-mail, log). |
| `infraestrutura` | `AgenteOdontoApplication`, `OdontoProperties`, `CasosDeUsoConfig` (cria os casos de uso sem anotações Spring na aplicação). |

Fluxo de uma mensagem: a Meta chama `POST /webhook/whatsapp` → valida a assinatura `X-Hub-Signature-256` → grava a mensagem (o `wa_message_id` único descarta reenvios) → responde **200 imediatamente** → depois do commit, um ouvinte assíncrono (virtual threads) processa a conversa com trava por número → envia as respostas pela Graph API. Detalhes nos [diagramas de sequência](docs/diagramas/README.md#7-sequência-webhook-e-processamento-assíncrono).

## Stack e versões

| Item | Versão |
|---|---|
| Java | 21 (virtual threads ligadas) |
| Spring Boot | 4.1.2 (`spring-boot-starter-webmvc`, `-restclient`, `-flyway`, `-data-jpa`, `-validation`, `-actuator`, `-mail`; Jackson 3) |
| Spring AI | 2.0.2 (BOM; `spring-ai-starter-model-anthropic` e `spring-ai-starter-model-openai`) |
| LLM | Claude (padrão `claude-haiku-4-5`, configurável em `ANTHROPIC_MODELO`) |
| Transcrição | OpenAI Whisper (`whisper-1`), opcional |
| Banco | PostgreSQL 18 + Flyway + Spring Data JPA (`ddl-auto: validate`) |
| WhatsApp | WhatsApp Business Platform – Cloud API (Graph API `v24.0`, configurável) |
| Testes | JUnit 5, AssertJ, Mockito, ArchUnit 1.5.1 |
| Empacotamento | Docker (um contêiner com a aplicação e o PostgreSQL) ou AWS Lambda (zip, SAM, SnapStart), Maven Wrapper |
| AWS (modo Lambda) | AWS SDK for Java 2.x, `aws-lambda-java-events`, `aws-serverless-java-container-springboot4` (só para a API admin) |

## Pré-requisitos

- Servidor Linux com **Docker** e **Docker Compose** (plugin `docker compose`), 2 GB de RAM ou mais.
- Um **domínio com HTTPS** apontando para o servidor. A Meta só chama webhooks HTTPS com certificado válido.
- Conta no **Meta for Developers** e um **número de telefone** dedicado ao WhatsApp Business Platform. O número não pode estar em uso no aplicativo WhatsApp comum.
- Chave da **Anthropic** (Claude). Opcional: chave da **OpenAI** para transcrever áudios.
- Para desenvolver: JDK 21 (o Maven Wrapper baixa o Maven).

## Configurando o WhatsApp na Meta

1. Em <https://developers.facebook.com/apps>, crie um aplicativo do tipo **Empresa** e adicione o produto **WhatsApp**.
2. Em **WhatsApp → Configuração da API**, adicione e verifique o número do consultório. Copie o **Phone number ID** para `WHATSAPP_PHONE_NUMBER_ID`.
3. Crie um **token permanente**. No **Gerenciador de Negócios → Usuários do sistema**, crie um usuário do sistema (admin), atribua o app e a conta do WhatsApp e gere um token com as permissões `whatsapp_business_messaging` e `whatsapp_business_management`. Coloque-o em `WHATSAPP_TOKEN_ACESSO`. O token temporário do painel expira em 24 horas.
4. Em **Configurações do app → Básico**, copie a **Chave secreta do aplicativo** (App Secret) para `WHATSAPP_SEGREDO_APP`. Ela valida a assinatura de cada webhook.
5. Em **WhatsApp → Configuração → Webhook**:
   - URL de callback: `https://seu-dominio/webhook/whatsapp`;
   - Token de verificação: o mesmo valor de `WHATSAPP_TOKEN_VERIFICACAO` (o `./odonto.sh configurar` gera um);
   - clique em **Verificar e salvar** (a aplicação precisa estar no ar) e **assine o campo `messages`**.
6. Em `WHATSAPP_NUMERO_CONSULTORIO`, coloque o WhatsApp que recebe os avisos, só com dígitos e DDI (ex.: `5511999998888`).
7. **Templates (recomendado).** A Meta só permite texto livre até 24 horas depois da última mensagem do destinatário. Se o celular do consultório não falar com o número do agente há mais de 24 horas, os avisos só chegam por template. Em **Gerenciador do WhatsApp → Modelos de mensagem**, crie em português (`pt_BR`), categoria **Utilidade**:

   | Nome | Corpo sugerido | Parâmetros (nesta ordem) |
   |---|---|---|
   | `aviso_agendamento` | `Nova consulta agendada pelo WhatsApp. Paciente: {{1}}. Data e horário: {{2}}. Problema: {{3}}. Prioridade: {{4}}.` | nome do paciente, data/hora, problema, prioridade |
   | `alerta_atendimento` | `Atenção: {{1}}. Paciente: {{2}}. Resumo: {{3}}.` | tipo do alerta, identificação, resumo |

   Depois da aprovação, use `WHATSAPP_USAR_TEMPLATE=true`. Com `false`, os avisos vão como texto livre.

## Instalação e operação

```bash
git clone https://github.com/rafaelzucon/agent-whatsapp-odonto.git
cd agent-whatsapp-odonto
./odonto.sh configurar   # cria o .env e gera chaves, senha do banco e tokens
nano .env                # preencha WhatsApp, ANTHROPIC_API_KEY, dados do consultório e agenda
./odonto.sh verificar    # confere as variáveis obrigatórias
./odonto.sh iniciar      # constrói a imagem, sobe o contêiner e espera ficar saudável
```

> **Guarde uma cópia segura do `.env`.** Sem `CRIPTO_CHAVE_DADOS` e `CRIPTO_CHAVE_HASH`, os dados criptografados (e os backups) não podem ser lidos.

| Comando | O que faz |
|---|---|
| `./odonto.sh configurar` | Cria o `.env` a partir do `.env.example` e gera chaves de criptografia, senha do banco e tokens |
| `./odonto.sh verificar` | Confere se as variáveis obrigatórias estão preenchidas |
| `./odonto.sh construir` | Constrói a imagem Docker |
| `./odonto.sh iniciar` | Constrói (se preciso), sobe o contêiner e espera ficar saudável |
| `./odonto.sh parar` / `reiniciar` | Para ou reinicia (os dados ficam no volume) |
| `./odonto.sh remover` | Remove o contêiner (o volume com o banco é mantido) |
| `./odonto.sh status` | Estado do contêiner e saúde da aplicação |
| `./odonto.sh logs [linhas]` | Acompanha os logs |
| `./odonto.sh backup` | Gera um `pg_dump` em `./backups` |
| `./odonto.sh restaurar <arquivo>` | Restaura um dump (antes, faz backup do estado atual) |
| `./odonto.sh atualizar` | `git pull`, backup, reconstrução e reinício |
| `./odonto.sh psql` | Abre o console do PostgreSQL |
| `./odonto.sh testar` | Roda os testes (`./mvnw verify`) |

O contêiner `agent-whatsapp-odonto` roda o PostgreSQL 18 (só em `127.0.0.1`, dentro do contêiner) e a aplicação (usuário sem privilégios, porta 8080). Os dados ficam no volume `agent-whatsapp-odonto-dados`. Para usar um PostgreSQL externo, defina `DB_URL`, `DB_USUARIO` e `DB_SENHA`; o banco embutido não é iniciado.

### HTTPS com proxy reverso

O jeito mais simples é o [Caddy](https://caddyserver.com/), que obtém o certificado sozinho. Exemplo de `Caddyfile`:

```caddy
agente.seuconsultorio.com.br {
    reverse_proxy 127.0.0.1:8080
}
```

Com Nginx, use `proxy_pass http://127.0.0.1:8080;` num `server` com TLS (Certbot). Outra opção sem abrir portas é o Cloudflare Tunnel. Para expor só o webhook, publique apenas `/webhook/whatsapp`; a API administrativa pode ficar restrita à rede interna.

## Implantação na AWS Lambda

Alternativa ao servidor com Docker: o mesmo código roda em funções AWS Lambda, sem servidor para manter. A pilha é descrita em [`aws/template.yaml`](aws/template.yaml) (AWS SAM) e implantada com [`aws/implantar.sh`](aws/implantar.sh). Diagrama: [11. Implantação na AWS Lambda](docs/diagramas/README.md#11-implantação-na-aws-lambda).

### Como funciona

| Função | Gatilho | O que faz |
|---|---|---|
| `webhook` | Function URL pública (HTTPS) | Responde ao desafio da Meta, valida a assinatura HMAC e enfileira as mensagens. Fica fora da VPC e não acessa o banco, para responder em milissegundos. |
| `conversa` | Fila SQS FIFO (lotes de até 10) | Executa a máquina de estados, grava no banco e responde pelo WhatsApp. |
| `manutencao` | EventBridge Scheduler | A cada 5 minutos expira conversas paradas; às 03:30 (Brasília) expurga mensagens antigas. |
| `admin` | Function URL (opcional, `HabilitarApiAdmin`) | A mesma [API administrativa](#api-administrativa), protegida pelo `ADMIN_TOKEN`. |
| `migracao` | Recurso personalizado do CloudFormation | Aplica as migrações do Flyway a cada implantação, antes de as outras funções mudarem de versão. |

- **Ordem e concorrência:** a fila FIFO usa o número do paciente como grupo, então as mensagens de uma conversa são processadas uma de cada vez e em ordem, mesmo com várias funções em paralelo. Isso substitui as travas em memória do modo servidor. A deduplicação usa o id da mensagem da Meta, e falhas parciais do lote são devolvidas à fila (`ReportBatchItemFailures`). Depois de 5 tentativas, a mensagem vai para a fila de falhas (DLQ).
- **Tarefas assíncronas:** avisos ao consultório e alertas, que no servidor rodam em segundo plano, são aguardados antes de a função terminar, para o Lambda não congelá-los no meio.
- **Segredos:** ficam em **um** segredo JSON do Secrets Manager, com as mesmas chaves das variáveis de ambiente (`WHATSAPP_TOKEN_ACESSO`, `ANTHROPIC_API_KEY`, `CRIPTO_CHAVE_DADOS`, `DB_SENHA`...). Os demais parâmetros vão como variáveis de ambiente das funções, geradas pelo template.
- **Inicialização rápida:** SnapStart (Java 21, arm64) guarda um snapshot com o Spring já iniciado; sem ele, a primeira mensagem depois de um tempo parado levaria cerca de 5 a 10 segundos.
- **Rede:** VPC própria com RDS PostgreSQL 18 em sub-redes privadas (cifrado, backup de 7 dias, proteção contra exclusão) e um NAT Gateway para as funções chamarem a Meta, a Anthropic e a OpenAI.
- **Mesmo código, outro empacotamento:** os handlers ficam em `infraestrutura/lambda` e os adaptadores sem dependência da AWS em `adaptador/entrada/lambda`; domínio e aplicação não mudam (o `ArquiteturaTest` verifica). Nas funções, o Spring sobe sem servidor web (só a `admin` usa o contêiner de servlet), sem o agendador interno (substituído pelo EventBridge) e sem o Flyway na inicialização (perfil `lambda`).

### Passo a passo

Pré-requisitos: conta AWS com credenciais configuradas (`aws configure` ou `aws sso login`), [AWS CLI v2](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html), [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html), Java 21, Python 3 e openssl.

```bash
./aws/implantar.sh configurar   # cria aws/parametros.env e aws/segredos.json (chaves, senha e tokens gerados)
# edite os campos PREENCHA: Phone Number ID, número do consultório, token da Meta, App Secret, chave da Anthropic...
./aws/implantar.sh verificar    # confere configuração, credenciais e template
./aws/implantar.sh implantar    # gera o zip, publica o segredo e implanta a pilha (~15 min na primeira vez, por causa do RDS)
```

No fim, o script mostra a `UrlWebhook`. Cadastre-a no painel da Meta (veja [Configurando o WhatsApp na Meta](#configurando-o-whatsapp-na-meta)) com o `WHATSAPP_TOKEN_VERIFICACAO` de `aws/segredos.json` e assine o campo `messages`. Os arquivos `aws/parametros.env` e `aws/segredos.json` ficam fora do Git; guarde uma cópia segura das chaves `CRIPTO_*`, pois sem elas os dados cifrados não podem ser lidos.

| Comando | Para quê |
|---|---|
| `./aws/implantar.sh implantar --construir` | Reconstrói o pacote (com testes) e implanta. Use depois de mudar código, parâmetros ou segredos. |
| `./aws/implantar.sh saidas` | URLs do webhook e da API admin, filas e endereço do banco. |
| `./aws/implantar.sh logs conversa 30m` | Acompanha os logs de uma função (`webhook`, `conversa`, `manutencao`, `admin`, `migracao`). |
| `./aws/implantar.sh falhas` | Quantas mensagens estão na fila de falhas. |
| `./aws/implantar.sh migrar` | Roda as migrações manualmente. |
| `./aws/implantar.sh remover` | Remove a pilha. O RDS deixa um snapshot final e o segredo é mantido. |

O pacote também pode ser gerado à parte com `./mvnw -Plambda package`, que produz `target/agent-whatsapp-odonto-lambda.zip` (cerca de 180 MB; o limite do Lambda é 250 MB descompactado, conferido no CI).

### Custos estimados

Para o volume de um consultório (centenas de mensagens por dia), Lambda, SQS e EventBridge ficam praticamente dentro do nível gratuito. O custo fixo vem do **NAT Gateway** (cerca de US$ 45/mês em São Paulo), do **RDS `db.t4g.micro`** com 20 GB (cerca de US$ 25/mês), do Secrets Manager (US$ 0,40/mês) e dos logs. O total fica na faixa de **US$ 70 a 80 por mês**, mais as APIs da Anthropic e da OpenAI. São valores aproximados: confira na [calculadora da AWS](https://calculator.aws/) para a sua região. Para reduzir custos, dá para trocar o NAT Gateway por uma instância NAT (ex.: fck-nat) ou usar a região `us-east-1`.

### Limitações do modo Lambda

- **Segredos congelados no snapshot:** o SnapStart guarda os segredos lidos na inicialização. Depois de trocar um segredo, rode `./aws/implantar.sh implantar`; o script detecta a mudança e publica novas versões. Se `DB_SENHA` mudar, ele também troca a senha mestre do RDS.
- **Mensagem perdida em timeout:** se a função `conversa` estourar o tempo (60 s) depois de registrar a mensagem como recebida, a nova tentativa a trata como duplicada. É o mesmo comportamento de uma queda do servidor no modo Docker.
- **Fila de falhas:** mensagens que falharam 5 vezes ficam 14 dias na DLQ. Vale criar um alarme do CloudWatch para `ApproximateNumberOfMessagesVisible` maior que zero.
- **Remoção:** o `remover` desliga a proteção contra exclusão do RDS antes de apagar a pilha; um snapshot final é mantido (e cobrado) até você apagá-lo.
- **SnapStart por região:** confira se o SnapStart para Java está disponível na região escolhida. Se não estiver, remova `SnapStart` do bloco `Globals` do template; tudo funciona, só com inicialização mais lenta.

### Teste local

As cinco funções foram testadas com a imagem oficial `public.ecr.aws/lambda/java:21` (Runtime Interface Emulator), PostgreSQL 18 e LocalStack (SQS FIFO e Secrets Manager), percorrendo cadastro, queixa, conflito de horário com sugestão, confirmação e aviso ao consultório. O CI repete um teste de fumaça do webhook no mesmo emulador.

## Variáveis de ambiente

Todas estão no [`.env.example`](.env.example). As principais:

| Variável | Obrigatória | Descrição |
|---|---|---|
| `CONSULTORIO_NOME`, `CONSULTORIO_ENDERECO` | sim | Usados nos textos ao paciente |
| `CONSULTORIO_FUSO` | não | Fuso da agenda (padrão `America/Sao_Paulo`) |
| `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_TOKEN_ACESSO` | sim | Envio pela Cloud API |
| `WHATSAPP_TOKEN_VERIFICACAO`, `WHATSAPP_SEGREDO_APP` | sim | Verificação e assinatura do webhook |
| `WHATSAPP_NUMERO_CONSULTORIO` | sim | Quem recebe os avisos |
| `WHATSAPP_VERSAO_API` | não | Versão da Graph API (padrão `v24.0`) |
| `WHATSAPP_USAR_TEMPLATE`, `WHATSAPP_TEMPLATE_*`, `WHATSAPP_IDIOMA_TEMPLATE` | não | Avisos por template aprovado |
| `ANTHROPIC_API_KEY`, `ANTHROPIC_MODELO` | sim | Claude (modelo padrão `claude-haiku-4-5`) |
| `IA_CONFIANCA_MINIMA` | não | Abaixo disso, vale a classificação por palavra-chave (padrão `0.6`) |
| `OPENAI_API_KEY`, `OPENAI_MODELO_TRANSCRICAO` | não | Transcrição de áudio; sem chave, a transcrição fica desligada |
| `AGENDA_DIAS`, `AGENDA_ABERTURA`, `AGENDA_FECHAMENTO` | não | Dias (`MONDAY,...`) e horário de atendimento |
| `AGENDA_ALMOCO_INICIO`, `AGENDA_ALMOCO_FIM` | não | Intervalo sem consultas |
| `AGENDA_DURACAO_CONSULTA`, `AGENDA_ANTECEDENCIA_MINIMA` | não | Ex.: `30m`, `2h` |
| `AGENDA_FERIADOS`, `AGENDA_DIAS_BUSCA` | não | Datas `AAAA-MM-DD` e quantos dias à frente procurar vaga |
| `CONVERSA_EXPIRACAO`, `CONVERSA_MAX_TENTATIVAS` | não | Inatividade que reinicia a conversa (`2h`) e respostas inválidas antes do atendimento humano (`3`) |
| `CONVERSA_CONFIRMAR_SUGESTAO` | não | `true`: pergunta antes de agendar o horário sugerido; `false`: agenda direto |
| `CRIPTO_CHAVE_DADOS`, `CRIPTO_CHAVE_HASH` | sim | Duas chaves **diferentes** de 32 bytes em Base64 (`openssl rand -base64 32`) |
| `DB_SENHA` | sim | Senha do PostgreSQL embutido |
| `ADMIN_TOKEN` | não | Token da API administrativa; vazio desliga a API |
| `RETENCAO_MENSAGENS_DIAS`, `RETENCAO_CONVERSAS_DIAS` | não | Expurgo automático (`180` e `30`) |
| `SMTP_*`, `EMAIL_REMETENTE`, `EMAIL_DESTINATARIOS` | não | Cópia dos avisos por e-mail |
| `PORTA_HOST`, `LOG_NIVEL`, `JAVA_OPTS` | não | Porta publicada, nível de log, opções da JVM |

## Personalizando os textos

Os textos ao paciente e as dicas de cada etapa ficam em [`src/main/resources/application.yml`](src/main/resources/application.yml), em `odonto.mensagens.textos` e `odonto.mensagens.dicas`. Marcadores substituídos automaticamente: `NOME_CONSULTORIO`, `NOME_PACIENTE`, `DATA_HORA`, `ENDERECO`, `PRONTUARIO` e `DICA`. Para mudar os textos sem recompilar, crie um `application.yml` só com as chaves alteradas (mesma estrutura, a partir de `odonto:`) e monte-o no contêiner em `/app/config/application.yml` (por exemplo, `volumes: ["./config:/app/config:ro"]` no `docker-compose.yml`). O Spring Boot carrega esse arquivo por cima do interno.

## API administrativa

Ativa só quando `ADMIN_TOKEN` está definido. Todas as rotas exigem `Authorization: Bearer <ADMIN_TOKEN>`. Datas e horas estão no fuso do consultório (ISO-8601, sem fuso).

| Método e rota | Descrição |
|---|---|
| `GET /api/admin/agenda?inicio=2026-11-20&fim=2026-11-27` | Consultas, bloqueios e horários livres do período |
| `POST /api/admin/agenda/consultas` | Agenda manualmente: `{"pacienteId":1,"queixaId":3,"inicio":"2026-11-20T09:00"}` |
| `POST /api/admin/agenda/consultas/{id}/cancelamento` | Cancela |
| `POST /api/admin/agenda/consultas/{id}/realizacao` | Marca como realizada |
| `POST /api/admin/agenda/consultas/{id}/falta` | Marca falta |
| `POST /api/admin/agenda/bloqueios` | Bloqueia período: `{"inicio":"2026-11-24T08:00","fim":"2026-11-24T12:00","motivo":"Congresso"}` |
| `DELETE /api/admin/agenda/bloqueios/{id}` | Remove bloqueio |
| `GET /api/admin/pacientes?nome=maria` | Busca por nome |
| `GET /api/admin/pacientes/rg?numero=123456789&uf=SP` | Busca por RG |
| `GET /api/admin/pacientes/{id}/prontuario` | Prontuário com as queixas |
| `PUT /api/admin/queixas/{id}/problema` | Reclassifica a queixa: `{"codigoProblema":"PULPITE"}` (origem passa a MANUAL) |
| `GET /api/admin/catalogo` | Catálogo de problemas |
| `GET /actuator/health` | Saúde (sem token) |

```bash
TOKEN=$(grep ^ADMIN_TOKEN= .env | cut -d= -f2-)
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/admin/agenda?inicio=$(date +%F)"
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"inicio":"2026-12-24T08:00","fim":"2026-12-24T18:00","motivo":"Véspera de Natal"}' \
     http://localhost:8080/api/admin/agenda/bloqueios
```

## Desenvolvimento e testes

```bash
./mvnw verify                 # compila e roda todos os testes (unitários + ArchUnit)
./mvnw test -Dtest=ConversaTest
```

- **Domínio:** testes puros da máquina de estados, da agenda (horário livre mais próximo, almoço, feriados, bloqueios), dos objetos de valor e das transições da consulta.
- **Aplicação:** casos de uso com Mockito e fakes em memória, incluindo o fluxo completo da conversa.
- **Adaptadores:** assinatura do webhook, conversão do payload da Meta, criptografia, adaptadores de IA com respostas simuladas.
- **Configuração:** o `application.yml` real é carregado e todos os textos são renderizados sem marcadores sobrando.
- **Arquitetura (`ArquiteturaTest`):** domínio só usa `java.*`; camadas respeitam a regra de dependência; aplicação sem Spring; Spring AI só nos adaptadores de IA e transcrição; JPA só na persistência; sem ciclos entre pacotes; controladores só falam com portas de entrada; sem injeção por campo.

Para rodar localmente sem Docker, suba um PostgreSQL 18 e informe `DB_URL=jdbc:postgresql://localhost:5432/odonto`, `DB_USUARIO` e `DB_SENHA`, além das demais variáveis, e rode `./mvnw spring-boot:run`. Para receber webhooks na sua máquina, use um túnel HTTPS (Cloudflare Tunnel, ngrok).

O CI do GitHub Actions ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) roda `./mvnw verify`, constrói a imagem Docker e faz um teste de fumaça no `/actuator/health`. Também gera o zip do Lambda, confere o limite de 250 MB, valida o template SAM (`cfn-lint`) e o `implantar.sh` (`shellcheck`) e chama o webhook no emulador oficial do Lambda.

## Segurança e LGPD

- **Assinatura do webhook:** toda chamada é validada com HMAC-SHA256 do App Secret, comparado em tempo constante. Assinatura inválida recebe 401.
- **Criptografia em repouso:** RG, nome, data de nascimento, responsável, textos das queixas, mensagens e dados da conversa são gravados com **AES-256-GCM** (`CRIPTO_CHAVE_DADOS`). A busca por RG usa **HMAC-SHA256** (`CRIPTO_CHAVE_HASH`), nunca o valor em claro.
- **Consentimento:** o cadastro só é criado depois do "SIM" do paciente, e a data do consentimento fica registrada. Com "NÃO", os dados coletados são descartados e o consultório é avisado.
- **Mínimo de exposição:** quem informa um RG já cadastrado vê só o primeiro nome do titular. Os logs não trazem nome, RG nem texto das mensagens, e o número de WhatsApp aparece mascarado.
- **Retenção:** o prontuário é mantido por pelo menos **20 anos** a partir do último registro (Lei 13.787/2018, art. 6º) e não é apagado automaticamente. Mensagens são expurgadas após `RETENCAO_MENSAGENS_DIAS` (180) e conversas encerradas após `RETENCAO_CONVERSAS_DIAS` (30).
- **API administrativa:** desligada sem `ADMIN_TOKEN`; o token é comparado em tempo constante.
- **IA:** recebe só o texto da queixa ou da resposta sobre data e horário (até 1.000 caracteres, delimitado como dado para resistir a injeção de instruções), nunca o nome nem o RG do cadastro, e não produz orientação clínica. Se a IA falhar ou tiver confiança baixa, vale a classificação por palavras-chave.
- **Contêiner:** a aplicação roda sem root; o PostgreSQL só escuta em `127.0.0.1` dentro do contêiner.

## Catálogo de problemas odontológicos

São 75 problemas em [`V2__catalogo_problemas.sql`](src/main/resources/db/migration/V2__catalogo_problemas.sql), cada um com código interno, nome, descrição, especialidade (CFO), prioridade (EMERGÊNCIA, URGÊNCIA ou ROTINA), referência CID-10 e palavras-chave. A prioridade define o tratamento: emergência gera alerta imediato e oferece o primeiro horário livre.

As fontes e as decisões de codificação estão em [`docs/fontes-catalogo.md`](docs/fontes-catalogo.md). Os códigos foram conferidos em cópias dos arquivos CID-10 do DATASUS e da OMS (2019). Alguns problemas comuns não têm código exato na CID-10 e usam o mais próximo, com comentário no SQL:

- dor de dente e restauração quebrada usam **K08.8**;
- avulsão usa **S03.2**;
- problemas de implante usam **T85.6** e **T85.7**;
- pericoronarite usa **K05.2**;
- bruxismo usa **F45.8**.

As classificações servem para triagem e organização da agenda, e o dentista pode corrigi-las pela API (`PUT /api/admin/queixas/{id}/problema`).

## Decisões e desvios em relação ao pedido

- **Etapa extra `AGUARDANDO_CONFIRMACAO_NOVA_CONSULTA`:** se o paciente identificado já tem consulta futura, o agente informa a data e pergunta se quer marcar outra, para evitar agendamentos duplicados por engano.
- **Mensagens e dicas extras:** além dos textos pedidos, há mensagens para conteúdo não textual, consulta existente, emergência e falta de horário livre, e uma dica por etapa, usada no "não entendi". Também foram criados os marcadores `PRONTUARIO` e `DICA`.
- **Preferência parcial vira sugestão:** com "amanhã à tarde", o agente escolhe o primeiro horário livre do período e pede confirmação, em vez de agendar direto.
- **Estados terminais silenciosos:** depois de encaminhar para atendimento humano ou de encerrar sem consentimento, o agente fica em silêncio até a conversa expirar, para não atrapalhar a equipe.
- **Parser determinístico antes da IA:** formatos comuns ("25/11 às 10h", "amanhã 14h", "sexta às 10h", "amanhã à tarde", "próxima segunda") são interpretados sem IA. A IA só entra nos demais casos.

## Limitações conhecidas

- **Uma instância só (modo Docker):** as travas por conversa ficam em memória. Para rodar mais de uma réplica seria preciso uma trava distribuída (ex.: `pg_advisory_lock`). No [modo Lambda](#implantação-na-aws-lambda) isso não se aplica: a fila FIFO serializa cada conversa. A reserva do horário já é protegida no banco (índice único parcial em `consulta.inicio` para consultas AGENDADAS).
- **Busca por nome em memória:** como o nome é cifrado, a busca administrativa decifra e filtra na aplicação. Funciona bem para o volume de um consultório, mas não para dezenas de milhares de pacientes.
- **Validação do RG:** o formato do RG varia por UF e não tem dígito verificador nacional. Por isso só são validados o tamanho (5 a 14 caracteres), os caracteres permitidos e a UF.
- **Webhook com JSON inválido** recebe 200 (e é registrado no log), para a Meta não ficar reenviando.
- **Áudio** depende da OpenAI; sem chave, o paciente é orientado a escrever.
- **Prontuário eletrônico:** este sistema registra a triagem. Para dispensar o papel, a Res. CFO-91/2009 exige sistema certificado (NGS2) e assinatura ICP-Brasil, o que está fora do escopo.

## Solução de problemas

| Sintoma | O que verificar |
|---|---|
| A Meta não verifica o webhook | URL HTTPS pública, `WHATSAPP_TOKEN_VERIFICACAO` igual ao do painel, `./odonto.sh status` saudável |
| Mensagens chegam, mas o agente não responde | Assinatura do campo `messages`; `WHATSAPP_SEGREDO_APP` correto (veja 401 nos logs); token de acesso válido |
| Erro 131047 / mensagem não entregue ao consultório | Fora da janela de 24h: crie os templates e use `WHATSAPP_USAR_TEMPLATE=true` |
| Erro 190 / token expirado | Use o token permanente do usuário do sistema |
| Queixas sempre classificadas por palavra-chave | `ANTHROPIC_API_KEY` e `ANTHROPIC_MODELO`; procure "Classificação por IA indisponível" nos logs |
| Paciente recebe "só consigo ler mensagens de texto" após um áudio | `OPENAI_API_KEY` vazia, então a transcrição fica desligada |
| Nenhum horário livre | `AGENDA_*`, feriados, bloqueios e `AGENDA_DIAS_BUSCA` |
| Falha ao iniciar com "chave" ou "criptografia" | `CRIPTO_CHAVE_DADOS` e `CRIPTO_CHAVE_HASH` precisam ser Base64 de 32 bytes e diferentes |

Logs: `./odonto.sh logs`. Saúde: `curl http://localhost:8080/actuator/health`.
