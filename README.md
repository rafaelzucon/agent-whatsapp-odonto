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
- [Princípios SOLID](#princípios-solid)
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
   → adaptador   (entrada: webhook, API admin, ouvintes, rotinas, Lambda | saída: JPA ou DynamoDB, WhatsApp, Claude, Whisper, e-mail)
      → aplicacao  (casos de uso *UseCase/*Service, portas de entrada e de saída)
         → dominio   (Java 21 puro: Paciente, Prontuario, Consulta, Conversa, Agenda...)
```

| Pacote (`br.com.consultorio.odonto`) | Responsabilidade |
|---|---|
| `dominio.modelo` | Modelo rico: `Paciente`, `Prontuario`, `QueixaRelatada`, `ProblemaOdontologico`, `Consulta`, `Conversa` (máquina de estados: `registrarResposta(...)` devolve uma `AcaoConversa`; cada etapa tem o seu `TratadorEtapa`, registrado no `FluxoConversa`), objetos de valor (`DocumentoRg`, `WaId`, `Horario`, `JanelaAtendimento`, `Prioridade`). |
| `dominio.servico` | `Agenda` (horário livre mais próximo), `ClassificadorPorPalavraChave`, `DetectorSinaisAlarme`, `InterpretadorDataHoraDeterministico`, `ValidadorDados`. |
| `aplicacao` | Casos de uso (`ConduzirConversa`, `IdentificarPaciente`, `CadastrarPaciente`, `RegistrarQueixa`, `AgendarConsulta`, `NotificarConsultorio`, `TranscreverAudio`, `GerenciarAgenda`...) e as portas que eles usam. Subpacotes `casodeuso.comando` (um `ExecutorComando` por comando da conversa), `casodeuso.interpretacao` (cadeia determinística → IA para data/hora) e `casodeuso.classificacao` (cadeia IA → palavras-chave para a queixa). Não depende de Spring. |
| `adaptador.entrada` | `WebhookController` (verificação e assinatura da Meta), `OuvinteEventos` (assíncrono, trava por conversa), `TarefasAgendadas`, controladores da API administrativa. |
| `adaptador.saida` | Persistência com criptografia em dois sabores, escolhidos por `odonto.persistencia`: JPA/PostgreSQL (`*JpaAdapter`, padrão) ou DynamoDB (`persistencia.dynamodb.*DynamoDbAdapter`, perfil `lambda`, catálogo lido do classpath), `WhatsAppCloudApiAdapter`, adaptadores Claude e Whisper (Spring AI), canais de notificação (WhatsApp, e-mail, log). |
| `infraestrutura` | `AgenteOdontoApplication`, `OdontoProperties`, `CasosDeUsoConfig` (composition root: cria os casos de uso sem anotações Spring na aplicação e escolhe/ordena as estratégias). |

Fluxo de uma mensagem: a Meta chama `POST /webhook/whatsapp` → valida a assinatura `X-Hub-Signature-256` → grava a mensagem (o `wa_message_id` único descarta reenvios) → responde **200 imediatamente** → depois do commit, um ouvinte assíncrono (virtual threads) processa a conversa com trava por número → envia as respostas pela Graph API. Detalhes nos [diagramas de sequência](docs/diagramas/README.md#7-sequência-webhook-e-processamento-assíncrono).

## Princípios SOLID

A Clean Architecture dá a estrutura; os princípios SOLID guiam o desenho das classes dentro de cada camada. A tabela mostra **onde se vê** cada princípio no código:

| Princípio | O que significa aqui | Onde se vê |
|---|---|---|
| **S** — Responsabilidade única (SRP) | Cada classe tem um único motivo para mudar. | <ul><li>`Conversa` cuida do ciclo de vida e das transições; a interpretação da resposta de cada etapa fica em um `TratadorEtapa` próprio (`TratadorRg`, `TratadorNome`, `TratadorConsentimento`, `TratadorProblema`...).</li><li>Um `ExecutorComando` por comando da conversa (`IdentificarPacienteExecutor`, `RegistrarQueixaExecutor`, `ReservarHorarioExecutor`...).</li><li>`RegistrarQueixaService` só orquestra classificar e persistir; a classificação está no `ClassificadorQueixa`.</li><li>`NotificarConsultorioService` só monta o aviso; o envio a vários canais e o isolamento de falhas ficam no `NotificacaoMultiCanal`.</li><li>`RespondedorPacienteWhatsApp` renderiza, envia e registra a resposta; `ConduzirConversaService` só conduz o fluxo.</li><li>Serviços de domínio focados: `Agenda`, `ValidadorDados`, `DetectorSinaisAlarme`, `ClassificadorPorPalavraChave`, `InterpretadorDataHoraDeterministico`.</li></ul> |
| **O** — Aberto/fechado (OCP) | Estender comportamento acrescentando classes, sem editar as existentes. | <ul><li>Etapa nova da conversa: um `TratadorEtapa` novo registrado no `FluxoConversa` (que falha na criação se alguma etapa ficar sem tratador).</li><li>Comando novo: um `ExecutorComando` novo registrado no `ProcessadorComandosPorTipo` (que falha na subida se algum `ComandoConversa` ficar sem executor ou tiver dois).</li><li>Nova forma de classificar a queixa ou de entender a data: uma `EstrategiaClassificacao` ou um `InterpretadorPreferenciaHorario` novo na cadeia (`ClassificadorQueixaEmCadeia`, `InterpretacaoEmCadeia`).</li><li>Novo canal de aviso ao consultório: um `CanalNotificacaoConsultorio` novo (bean Spring), entregue ao `NotificacaoMultiCanal`.</li><li>Novo banco: o DynamoDB entrou como um conjunto novo de adaptadores (`persistencia.dynamodb`), ligado por `@PersistenciaDynamoDb`; os adaptadores JPA (marcados com `@PersistenciaJpa`), os casos de uso e o domínio não mudaram.</li><li>Ordem e escolha das estratégias ficam só no `CasosDeUsoConfig`.</li></ul> |
| **L** — Substituição de Liskov (LSP) | Qualquer implementação de uma porta pode substituir outra sem surpresas. | <ul><li>Os canais `WhatsAppCanalNotificacao`, `EmailCanalNotificacao` e `LogCanalNotificacao` seguem o mesmo contrato: `habilitado()` diz se podem ser chamados e falhas viram `FalhaIntegracaoException` (o canal do WhatsApp sem número configurado também — `WhatsAppCanalNotificacaoTest`).</li><li>O próprio `NotificacaoMultiCanal` é um `CanalNotificacaoConsultorio` (composite) e pode ser usado onde se espera um canal.</li><li>Adaptadores de IA, transcrição e WhatsApp traduzem erros técnicos para `FalhaIntegracaoException`, documentada nas portas; os casos de uso aplicam o mesmo fallback qualquer que seja a implementação.</li><li>Os fakes em memória dos testes (`Fakes`) substituem os adaptadores JPA, WhatsApp e Claude sem mudar os casos de uso.</li><li>`PacienteDynamoDbAdapter`, `ConsultaDynamoDbAdapter` e `MensagemDynamoDbAdapter` preservam o contrato dos equivalentes JPA: RG repetido, horário ocupado e mensagem reenviada levantam as mesmas exceções de domínio (`HorarioIndisponivelException`, `MensagemDuplicadaException`), e falhas desfazem a transação inteira (`DynamoDbAdaptersTest`).</li></ul> |
| **I** — Segregação de interfaces (ISP) | Clientes dependem só dos métodos que usam. | <ul><li>`EnviadorWhatsApp` é a soma de `EnviadorTextoWhatsApp` (usado pelo `RespondedorPacienteWhatsApp`) e `EnviadorTemplateWhatsApp`.</li><li>A remoção por retenção saiu dos repositórios: `RetencaoMensagens` e `RetencaoConversas` são usadas só por `ExpurgarMensagensService` e `ExpirarConversasService`; a conversa usa `ConversaRepositorio` sem métodos de expurgo.</li><li>`ProvedorMidia` separado do envio de mensagens, embora o `WhatsAppCloudApiAdapter` implemente os dois.</li><li>`ContextoEtapa` expõe ao tratador só o que ele precisa da `Conversa` (dados, etapa, tentativa inválida, alerta).</li><li>Uma porta de entrada por caso de uso (`*UseCase`), para que cada adaptador de entrada veja só o que chama.</li></ul> |
| **D** — Inversão de dependência (DIP) | Políticas de alto nível dependem de abstrações; detalhes implementam essas abstrações. | <ul><li>Portas de saída (`aplicacao.porta.saida`) definidas pela aplicação e implementadas pelos adaptadores (JPA, DynamoDB, WhatsApp, Claude, Whisper, e-mail).</li><li>Os casos de uso dependem da porta `ExecutorTransacional`; o `SpringExecutorTransacional` depende só do `PlatformTransactionManager` abstrato, que no modo Docker é o do JPA e no Lambda é o `GerenciadorTransacaoDynamoDb` (acumula as escritas e as grava num único `TransactWriteItems`).</li><li>`ConduzirConversaService` depende das interfaces `ProcessadorComandosConversa` e `RespondedorPaciente`; `RegistrarQueixaService` de `ClassificadorQueixa`; `NotificarConsultorioService` de `CanalNotificacaoConsultorio`.</li><li>Domínio e aplicação não têm anotações nem dependências de framework; a composição acontece no `CasosDeUsoConfig`, com injeção pelo construtor.</li><li>Verificado pelo `ArquiteturaTest`: regra de dependência entre camadas, aplicação sem Spring e a regra `dependencias_apontam_para_abstracoes` (campos de casos de uso e adaptadores não podem ter o tipo de uma classe concreta que implementa uma interface do sistema).</li></ul> |

Pontos mantidos de propósito: o `switch` sobre a tarefa no `TarefaManutencaoAdapter` (o enum é fechado e o compilador avisa quando falta um caso) e os repositórios por agregado (`PacienteRepositorio`, `ConsultaRepositorio`), cujos métodos são usados pelos mesmos casos de uso.

## Stack e versões

| Item | Versão |
|---|---|
| Java | 21 (virtual threads ligadas) |
| Spring Boot | 4.1.2 (`spring-boot-starter-webmvc`, `-restclient`, `-flyway`, `-data-jpa`, `-validation`, `-actuator`, `-mail`; Jackson 3) |
| Spring AI | 2.0.2 (BOM; `spring-ai-starter-model-anthropic` e `spring-ai-starter-model-openai`) |
| LLM | Claude (padrão `claude-haiku-4-5`, configurável em `ANTHROPIC_MODELO`) |
| Transcrição | OpenAI Whisper (`whisper-1`), opcional |
| Banco | PostgreSQL 18 + Flyway + Spring Data JPA (`ddl-auto: validate`) no modo Docker; Amazon DynamoDB sob demanda (AWS SDK v2) no modo Lambda |
| WhatsApp | WhatsApp Business Platform – Cloud API (Graph API `v24.0`, configurável) |
| Testes | JUnit 5, AssertJ, Mockito, ArchUnit 1.5.1 |
| Empacotamento | Docker (um contêiner com a aplicação e o PostgreSQL) ou AWS Lambda (zip, SAM, SnapStart), Maven Wrapper |
| AWS (modo Lambda) | AWS SDK for Java 2.x (DynamoDB, SQS, SSM Parameter Store), `aws-lambda-java-events`, `aws-serverless-java-container-springboot4` (só para a API admin) |
| Testes do modo Lambda | Testcontainers + DynamoDB Local (`amazon/dynamodb-local`) |

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

Alternativa ao servidor com Docker: o mesmo código roda em funções AWS Lambda, **sem servidor ligado 24h e na configuração de menor custo** (na faixa de **US$ 0 a 2 por mês** para um consultório, sem contar as APIs de IA). A pilha é descrita em [`aws/template.yaml`](aws/template.yaml) (AWS SAM) e implantada com [`aws/implantar.sh`](aws/implantar.sh). Diagrama: [11. Implantação na AWS Lambda](docs/diagramas/README.md#11-implantação-na-aws-lambda).

### Como funciona

| Função | Gatilho | O que faz |
|---|---|---|
| `webhook` (512 MB) | Function URL pública (HTTPS) | Responde ao desafio da Meta, valida a assinatura HMAC e enfileira as mensagens. Não sobe o Spring nem acessa o banco, para responder em milissegundos. |
| `conversa` (1 GB) | Fila SQS FIFO (lotes de até 10) | Executa a máquina de estados, grava no DynamoDB e responde pelo WhatsApp. |
| `manutencao` (1 GB) | EventBridge Scheduler | A cada 15 minutos expira conversas paradas; às 03:30 (Brasília) expurga mensagens antigas. |
| `admin` (1 GB) | Function URL (opcional, `HabilitarApiAdmin`) | A mesma [API administrativa](#api-administrativa), protegida pelo `ADMIN_TOKEN`. |

- **Ordem e concorrência:** a fila FIFO usa o número do paciente como grupo, então as mensagens de uma conversa são processadas uma de cada vez e em ordem. Isso substitui as travas em memória do modo servidor. A deduplicação usa o id da mensagem da Meta, e falhas parciais do lote voltam para a fila (`ReportBatchItemFailures`). Depois de 5 tentativas, a mensagem vai para a fila de falhas (DLQ).
- **Dados no DynamoDB:** uma tabela única (`<pilha>-dados`) cobrada por requisição, gravada pelos `*DynamoDbAdapter`, que implementam as mesmas portas dos adaptadores JPA, com a mesma criptografia (AES-GCM e HMAC do RG). RG repetido, horário ocupado e mensagem reenviada são barrados por itens de trava gravados na mesma transação (`TransactWriteItems`), no papel dos índices únicos do PostgreSQL. O catálogo de 75 problemas é lido do próprio `V2__catalogo_problemas.sql` empacotado, então não há migrações a aplicar. A tabela tem backup contínuo (PITR, 35 dias), proteção contra exclusão e é **mantida** se a pilha for removida. Chaves e índices: [modelo de dados](docs/diagramas/README.md#9-modelo-de-dados-postgresql).
- **Segredos no Parameter Store:** cada chave de `aws/segredos.json` vira um parâmetro `SecureString` em `/<pilha>/segredos/<CHAVE>` (camada padrão, cifrada com a chave gerenciada `aws/ssm`, sem custo). Os nomes são os mesmos das variáveis de ambiente (`WHATSAPP_TOKEN_ACESSO`, `ANTHROPIC_API_KEY`, `CRIPTO_CHAVE_DADOS`...). Os demais parâmetros vão como variáveis de ambiente das funções, geradas pelo template.
- **Sem VPC e sem NAT:** as funções falam com DynamoDB, SQS e Parameter Store pelos endpoints públicos da AWS (HTTPS, autorizados por IAM) e chamam a Meta, a Anthropic e a OpenAI direto pela internet. Nada fica exposto além das duas Function URLs: a do webhook (validada pela assinatura da Meta) e a da API admin (validada pelo token).
- **Inicialização rápida:** SnapStart (Java 21, arm64), sem custo extra no runtime Java, guarda um snapshot com o Spring já iniciado. Sem ele, a primeira mensagem depois de um tempo parado levaria de 5 a 10 segundos.
- **Mesmo código, outro empacotamento:** os handlers ficam em `infraestrutura/lambda`, e os adaptadores sem dependência da AWS em `adaptador/entrada/lambda`. Domínio e aplicação não mudam (o `ArquiteturaTest` verifica). No perfil `lambda`, o Spring sobe sem servidor web (só a `admin` usa o contêiner de servlet), sem agendador interno (substituído pelo EventBridge), sem DataSource, JPA nem Flyway, e com `odonto.persistencia=dynamodb`, que liga os adaptadores DynamoDB no lugar dos JPA.

### Decisões de menor custo

| Decisão | Alternativa descartada | Economia aproximada (sa-east-1) |
|---|---|---|
| DynamoDB sob demanda (paga por requisição; 25 GB de armazenamento grátis) | RDS PostgreSQL `db.t4g.micro` ligado 24h | ~US$ 25/mês |
| Funções fora de VPC | VPC com NAT Gateway para sair à internet | ~US$ 45/mês |
| Parameter Store, camada padrão | Secrets Manager (US$ 0,40 por segredo/mês) | US$ 0,40/mês ou mais |
| Function URL | API Gateway | Function URL não tem custo |
| Graviton (`arm64`) | x86_64 | ~20% no tempo de execução |
| Webhook com 512 MB; expiração a cada 15 min; concorrência da conversa = 2 | 1 GB; a cada 5 min; 5 | Menos GB-segundo e execuções |
| Logs retidos por 7 dias | 30 dias ou para sempre | Armazenamento de logs |
| SnapStart | Concorrência provisionada | A concorrência provisionada é cobrada 24h |

### Custos estimados

Para um consultório com cerca de 300 mensagens por dia (~9 mil por mês):

| Serviço | Uso aproximado no mês | Custo |
|---|---|---|
| Lambda | ~25 mil invocações e ~15 mil GB-s (nível gratuito permanente: 1 milhão de invocações e 400 mil GB-s) | US$ 0 |
| SQS FIFO | Mensagens e consultas da fila pelo Lambda, inclusive ociosa (~700 mil; grátis até 1 milhão) | US$ 0 |
| DynamoDB sob demanda | ~100 mil gravações e ~200 mil leituras; menos de 1 GB guardado (grátis até 25 GB) | ~US$ 0,20 a 0,50 |
| Backup contínuo (PITR) | Proporcional ao tamanho da tabela (menos de 1 GB) | ~US$ 0,20 |
| Parameter Store (padrão) | ~9 parâmetros, lidos na publicação de cada versão | US$ 0 |
| EventBridge Scheduler | ~3 mil execuções (grátis até 14 milhões) | US$ 0 |
| CloudWatch Logs | Poucos MB (grátis até 5 GB) | US$ 0 |
| S3 (pacote do SAM, ~190 MB por versão) | Bucket criado pelo `sam deploy` | ~US$ 0,01 a 0,10 |
| **Total AWS** | | **~US$ 0 a 2/mês** |

Não estão incluídos os custos da Anthropic (Claude), da OpenAI (Whisper, opcional) e das conversas cobradas pela Meta. Com volume muito maior, o DynamoDB e o Lambda crescem proporcionalmente ao uso, sem degraus. São valores aproximados, e o nível gratuito pode mudar: confira na [calculadora da AWS](https://calculator.aws/) para a sua região. A região `us-east-1` (Virgínia) é um pouco mais barata que `sa-east-1` (São Paulo), mas guarda os dados fora do Brasil.

### Passo a passo completo na AWS

#### 1. Criar a conta AWS

1. Acesse <https://aws.amazon.com/pt/> e clique em **Criar uma conta da AWS**.
2. Informe um e-mail (de preferência do consultório, não pessoal) e um nome para a conta (ex.: `consultorio-odonto`). Confirme o código enviado ao e-mail e defina a senha do **usuário raiz (root)**.
3. Preencha os dados de contato (tipo **Profissional** se houver CNPJ) e um **cartão de crédito**. A AWS pode fazer uma cobrança de verificação de US$ 1, estornada depois.
4. Confirme o telefone por SMS ou ligação.
5. Se aparecer a escolha de plano, o **gratuito** serve para testar. Para produção, prefira o **pago**: os serviços desta pilha continuam dentro do nível gratuito permanente, e a conta não fica sujeita aos limites e ao prazo do plano gratuito. No suporte, escolha **Basic (gratuito)**.
6. Aguarde o e-mail de ativação (em geral, minutos).

#### 2. Proteger a conta raiz

1. Entre no [console](https://console.aws.amazon.com/) como usuário raiz.
2. Clique no nome da conta (canto superior direito) → **Credenciais de segurança** → **Atribuir dispositivo de MFA**. Use um aplicativo autenticador ou uma chave de segurança.
3. Não crie chaves de acesso para o usuário raiz. Ele só deve ser usado para tarefas de cobrança e de conta.

#### 3. Criar um alerta de custos (recomendado)

1. No console, abra **Billing and Cost Management** → **Budgets** → **Criar orçamento**.
2. Escolha o modelo **Orçamento de gasto zero** (avisa no primeiro centavo) ou **Orçamento de custo mensal** com, por exemplo, US$ 5.
3. Informe o e-mail que recebe os alertas e salve. Orçamentos sem ações automáticas são gratuitos.

#### 4. Criar um usuário administrador para o dia a dia

Recomendado: **IAM Identity Center** (login com credenciais temporárias, sem chaves fixas no computador).

1. No console, selecione a região que vai usar (ex.: **América do Sul (São Paulo) sa-east-1**) e abra **IAM Identity Center** → **Ativar**.
2. **Usuários** → **Adicionar usuário**: informe nome de usuário, e-mail, nome e sobrenome. O usuário recebe um convite por e-mail para criar a senha e cadastrar o MFA.
3. **Conjuntos de permissões** → **Criar conjunto de permissões** → **Conjunto predefinido** → `AdministratorAccess`.
4. **Contas da AWS** → marque a conta → **Atribuir usuários ou grupos** → escolha o usuário e o conjunto `AdministratorAccess`.
5. Anote a **URL do portal de acesso** (em **Configurações**, algo como `https://d-xxxxxxxxxx.awsapps.com/start`).

Alternativa mais simples (menos segura): **IAM** → **Usuários** → **Criar usuário** com a política `AdministratorAccess`, ativar MFA e criar uma **chave de acesso** do tipo CLI. Guarde a chave com cuidado e desative-a quando não precisar mais.

#### 5. Instalar as ferramentas

| Ferramenta | Instalação | Conferir |
|---|---|---|
| AWS CLI v2 | [Guia oficial](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html). No Linux: `curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o awscliv2.zip && unzip awscliv2.zip && sudo ./aws/install` | `aws --version` |
| AWS SAM CLI | [Guia oficial](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html). No macOS: `brew install aws-sam-cli` | `sam --version` |
| Java 21 | [Eclipse Temurin 21](https://adoptium.net/) ou `sdk install java 21-tem` | `java -version` |
| Python 3, openssl, git | Gerenciador de pacotes do sistema (`sudo apt install python3 openssl git unzip`) | `python3 --version` |

No Windows, use o **WSL 2** (Ubuntu) e siga os comandos do Linux. Docker **não** é necessário para implantar.

#### 6. Configurar o acesso da CLI

Com o IAM Identity Center:

```bash
aws configure sso
#   SSO session name: odonto
#   SSO start URL: https://d-xxxxxxxxxx.awsapps.com/start   (anotada no passo 4)
#   SSO region: sa-east-1
#   (o navegador abre para você entrar e autorizar)
#   CLI default client Region: sa-east-1
#   CLI default output format: json
#   CLI profile name: odonto
export AWS_PROFILE=odonto        # repita em cada terminal novo (ou coloque no ~/.bashrc)
aws sso login                    # quando a sessão expirar
aws sts get-caller-identity      # deve mostrar o número da conta
```

Com chave de acesso de usuário IAM: `aws configure` (informe a chave, o segredo e a região `sa-east-1`).

#### 7. Preparar as contas externas

- **WhatsApp (Meta):** siga [Configurando o WhatsApp na Meta](#configurando-o-whatsapp-na-meta) até ter o **Phone number ID**, o **token permanente** e o **App Secret**. O webhook só é cadastrado no passo 11.
- **Anthropic:** crie uma chave em <https://console.anthropic.com/>.
- **OpenAI (opcional, para áudios):** crie uma chave em <https://platform.openai.com/>.

#### 8. Baixar o projeto e gerar a configuração

```bash
git clone https://github.com/rafaelzucon/agent-whatsapp-odonto.git
cd agent-whatsapp-odonto
./aws/implantar.sh configurar
```

O comando cria `aws/parametros.env` e `aws/segredos.json` (com permissão `600`), já com as chaves de criptografia, o token da API admin e o token de verificação do webhook gerados aleatoriamente. Os dois arquivos ficam fora do Git. **Guarde uma cópia segura de `aws/segredos.json`** (ex.: num gerenciador de senhas): sem as chaves `CRIPTO_*`, os dados cifrados não podem ser lidos.

#### 9. Preencher os parâmetros e os segredos

Edite os valores marcados com `PREENCHA`.

`aws/parametros.env` (sem segredos):

| Chave | O que colocar |
|---|---|
| `PILHA` | Nome da pilha, prefixo de todos os recursos (padrão `agente-odonto`) |
| `REGIAO` | `sa-east-1` (São Paulo) ou outra região |
| `WhatsAppPhoneNumberId` | Phone number ID da Meta |
| `WhatsAppNumeroConsultorio` | Número que recebe os avisos (DDI + DDD + número, só dígitos) |
| `ConsultorioNome`, `ConsultorioEndereco` | Usados nos textos ao paciente |
| `TranscricaoProvedor` | `openai` (com `OPENAI_API_KEY`) ou `none` |
| `ConcorrenciaConversa` | Instâncias simultâneas da conversa (padrão 2, o mínimo) |
| `HabilitarApiAdmin` | `true` cria a função e a URL da API admin |
| `HabilitarBackupContinuo` | `true` liga o PITR da tabela (recomendado) |
| `DiasRetencaoLogs` | Dias de retenção dos logs (padrão 7) |

`aws/segredos.json`:

| Chave | O que colocar |
|---|---|
| `WHATSAPP_TOKEN_ACESSO` | Token permanente do usuário do sistema da Meta |
| `WHATSAPP_SEGREDO_APP` | App Secret do aplicativo da Meta |
| `ANTHROPIC_API_KEY` | Chave da Anthropic |
| `OPENAI_API_KEY` | Chave da OpenAI, ou `""` sem transcrição |
| `WHATSAPP_TOKEN_VERIFICACAO`, `ADMIN_TOKEN`, `CRIPTO_CHAVE_DADOS`, `CRIPTO_CHAVE_HASH` | Já gerados; não altere depois de ter dados gravados |

Outras variáveis da [tabela de variáveis](#variáveis-de-ambiente) (ex.: `AGENDA_*`) também podem ser incluídas em `aws/segredos.json`: elas viram parâmetros e são lidas pelas funções com o mesmo nome.

#### 10. Verificar e implantar

```bash
./aws/implantar.sh verificar    # arquivos, credenciais, ferramentas e template (sam validate --lint)
./aws/implantar.sh implantar    # testa e gera o zip, publica os segredos e cria a pilha
```

O `implantar`:

1. roda `./mvnw -Plambda package` (testes incluídos) e confere o limite de 250 MB do pacote (cerca de 200 MB);
2. sincroniza `aws/segredos.json` com o Parameter Store, gravando as chaves novas ou alteradas e apagando as removidas;
3. calcula a versão (hash do pacote, do template, dos parâmetros e dos segredos) e roda o `sam deploy`, que envia o pacote para um bucket S3 gerenciado pelo SAM e cria a tabela, as filas, as funções, os agendamentos e os logs.

A primeira implantação leva de 5 a 10 minutos (a maior parte é o envio do pacote e a criação dos snapshots do SnapStart). No fim, o script mostra as saídas: `UrlWebhook`, `UrlApiAdmin`, `TabelaDynamoDb`, `CaminhoSegredos` e as filas.

#### 11. Cadastrar o webhook na Meta

No painel do aplicativo, em **WhatsApp → Configuração → Webhook → Editar**:

- **URL de callback:** a `UrlWebhook` (ex.: `https://abc123.lambda-url.sa-east-1.on.aws/`);
- **Token de verificação:** o `WHATSAPP_TOKEN_VERIFICACAO` de `aws/segredos.json`;
- clique em **Verificar e salvar** e, em **Campos do webhook**, assine `messages`.

#### 12. Testar

1. De outro celular, mande "Oi" para o número do consultório e siga a conversa até agendar.
2. Acompanhe: `./aws/implantar.sh logs conversa` (ou `logs webhook`, `logs manutencao`, `logs admin`).
3. Consulte a agenda pela API admin:

```bash
URL=$(aws cloudformation describe-stacks --stack-name agente-odonto \
      --query "Stacks[0].Outputs[?OutputKey=='UrlApiAdmin'].OutputValue" --output text)
TOKEN=$(python3 -c 'import json; print(json.load(open("aws/segredos.json"))["ADMIN_TOKEN"])')
curl -s -H "Authorization: Bearer $TOKEN" "${URL}api/admin/agenda?inicio=$(date +%F)"
```

#### 13. Alarme da fila de falhas (opcional, gratuito até 10 alarmes)

```bash
TOPICO=$(aws sns create-topic --name agente-odonto-alertas --query TopicArn --output text)
aws sns subscribe --topic-arn "$TOPICO" --protocol email --notification-endpoint voce@exemplo.com.br   # confirme no e-mail
aws cloudwatch put-metric-alarm --alarm-name agente-odonto-falhas \
  --namespace AWS/SQS --metric-name ApproximateNumberOfMessagesVisible \
  --dimensions Name=QueueName,Value=agente-odonto-mensagens-falhas.fifo \
  --statistic Maximum --period 300 --evaluation-periods 1 --threshold 0 \
  --comparison-operator GreaterThanThreshold --treat-missing-data notBreaching --alarm-actions "$TOPICO"
```

### Operação

| Comando | Para quê |
|---|---|
| `./aws/implantar.sh implantar --construir` | Reconstrói o pacote (com testes) e implanta. Use depois de mudar código, parâmetros ou segredos. |
| `./aws/implantar.sh saidas` | URLs do webhook e da API admin, tabela, caminho dos segredos e filas. |
| `./aws/implantar.sh logs conversa 30m` | Acompanha os logs de uma função (`webhook`, `conversa`, `manutencao`, `admin`). |
| `./aws/implantar.sh falhas` | Quantas mensagens estão na fila de falhas. |
| `./aws/implantar.sh backup` | Cria um backup sob demanda da tabela (além do PITR). |
| `./aws/implantar.sh remover` | Remove a pilha. A tabela e os segredos são mantidos; o script mostra como apagá-los. |

- **Trocar um segredo ou parâmetro:** edite `aws/segredos.json` ou `aws/parametros.env` e rode `./aws/implantar.sh implantar`. Como o SnapStart congela a configuração no snapshot, o script muda a versão e publica funções novas. Editar o parâmetro direto no console **não** tem efeito até a próxima implantação.
- **Restaurar um backup:** `aws dynamodb restore-table-to-point-in-time --source-table-name agente-odonto-dados --target-table-name agente-odonto-dados-restaurada --restore-date-time 2026-11-18T10:00:00Z` cria uma tabela nova com os dados daquele instante. Use-a para consultar ou recuperar itens.
- **Pacote à parte:** `./mvnw -Plambda package` gera `target/agent-whatsapp-odonto-lambda.zip` (cerca de 200 MB descompactado; o limite do Lambda é 250 MB, conferido no CI).

### Limitações do modo Lambda

- **Configuração congelada no snapshot:** veja "Trocar um segredo ou parâmetro" acima.
- **Mensagem perdida em timeout:** se a função `conversa` estourar o tempo (60 s) depois de registrar a mensagem como recebida, a nova tentativa a trata como duplicada. É o mesmo comportamento de uma queda do servidor no modo Docker.
- **Fila de falhas:** mensagens que falharam 5 vezes ficam 14 dias na DLQ (veja o alarme do passo 13).
- **Bases separadas:** os modos Docker (PostgreSQL) e Lambda (DynamoDB) não compartilham dados. Para migrar pacientes entre eles, use a API administrativa ou um script próprio.
- **Function URLs públicas:** a do webhook só aceita chamadas assinadas pela Meta, e a da API admin exige o `ADMIN_TOKEN`. Para desligar a API admin, use `HabilitarApiAdmin=false`.
- **SnapStart por região:** confira se o SnapStart para Java está disponível na região escolhida. Se não estiver, remova `SnapStart` do bloco `Globals` do template; tudo funciona, só com inicialização mais lenta.

### Testes do modo Lambda

- `DynamoDbAdaptersTest` exercita os seis adaptadores DynamoDB no [DynamoDB Local](https://hub.docker.com/r/amazon/dynamodb-local) via Testcontainers. Cobre RG e horário duplicados, mensagem reenviada, rollback da transação, agenda, retenção e expiração de conversas. É pulado sem Docker.
- `PerfilLambdaTest` sobe o contexto do perfil `lambda` e confere que só os adaptadores DynamoDB são criados, sem DataSource.
- O CI gera o zip, valida o template (`cfn-lint`) e o script (`shellcheck`) e chama o webhook no emulador oficial do Lambda (`public.ecr.aws/lambda/java:21`).

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
- **Persistência DynamoDB:** `DynamoDbAdaptersTest` roda os adaptadores no DynamoDB Local (Testcontainers; pulado sem Docker). `PerfilLambdaTest` confere a montagem do perfil `lambda`.
- **Arquitetura (`ArquiteturaTest`):** domínio só usa `java.*`; camadas respeitam a regra de dependência; aplicação sem Spring; Spring AI só nos adaptadores de IA e transcrição; JPA só na persistência; SDK da AWS só na infraestrutura do Lambda e em `persistencia.dynamodb`; sem ciclos entre pacotes; controladores só falam com portas de entrada; campos de casos de uso e adaptadores guardam abstrações, nunca implementações do sistema (DIP); sem injeção por campo.

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
- **AWS:** tabela DynamoDB cifrada em repouso pela AWS (além da criptografia da aplicação), com backup contínuo e proteção contra exclusão; segredos como `SecureString` no Parameter Store; cada função só recebe as permissões IAM de que precisa (fila, tabela e o caminho `/<pilha>/segredos`); dados em `sa-east-1` (Brasil) por padrão.

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

- **Uma instância só (modo Docker):** as travas por conversa ficam em memória. Para rodar mais de uma réplica seria preciso uma trava distribuída (ex.: `pg_advisory_lock`). No [modo Lambda](#implantação-na-aws-lambda) isso não se aplica: a fila FIFO serializa cada conversa. A reserva do horário já é protegida no banco (índice único parcial em `consulta.inicio` para consultas AGENDADAS no PostgreSQL; item de trava `HORARIO#<início>` gravado na mesma transação no DynamoDB).
- **Busca por nome em memória:** como o nome é cifrado, a busca administrativa decifra e filtra na aplicação (nos dois bancos). Funciona bem para o volume de um consultório, mas não para dezenas de milhares de pacientes.
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
| Lambda: a Meta não verifica o webhook | `UrlWebhook` completa (com a barra final); token igual ao de `aws/segredos.json`; `./aws/implantar.sh logs webhook` |
| Lambda: segredo trocado não tem efeito | Rode `./aws/implantar.sh implantar` (o SnapStart congela a configuração) |
| Lambda: `AccessDeniedException` ou `ResourceNotFoundException` do DynamoDB | Pilha implantada na mesma região do `aws/parametros.env`; `./aws/implantar.sh saidas` mostra a tabela |
| Falha ao iniciar com "chave" ou "criptografia" | `CRIPTO_CHAVE_DADOS` e `CRIPTO_CHAVE_HASH` precisam ser Base64 de 32 bytes e diferentes |

Logs: `./odonto.sh logs`. Saúde: `curl http://localhost:8080/actuator/health`.
