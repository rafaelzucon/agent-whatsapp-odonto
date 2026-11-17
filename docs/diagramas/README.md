# Diagramas

Diagramas em [Mermaid](https://mermaid.js.org/), renderizados automaticamente pelo GitHub. As fontes ficam em [`fonte/`](fonte/) (arquivos `.mmd`) e podem ser exportadas para PNG/SVG com o [Mermaid CLI](https://github.com/mermaid-js/mermaid-cli):

```bash
npx -p @mermaid-js/mermaid-cli mmdc -i docs/diagramas/fonte/04-maquina-estados-conversa.mmd -o maquina-estados.svg
```

## Índice

1. [Contexto do sistema](#1-contexto-do-sistema)
2. [Camadas da Clean Architecture](#2-camadas-da-clean-architecture)
3. [Componentes, portas e adaptadores](#3-componentes-portas-e-adaptadores)
4. [Máquina de estados da conversa](#4-máquina-de-estados-da-conversa)
5. [Sequência: primeiro contato e cadastro](#5-sequência-primeiro-contato-e-cadastro)
6. [Sequência: queixa e agendamento](#6-sequência-queixa-e-agendamento)
7. [Sequência: webhook e processamento assíncrono](#7-sequência-webhook-e-processamento-assíncrono)
8. [Classes do domínio](#8-classes-do-domínio)
9. [Modelo de dados (PostgreSQL)](#9-modelo-de-dados-postgresql)
10. [Implantação](#10-implantação)

## 1. Contexto do sistema

Atores e sistemas externos: paciente, Meta (WhatsApp Cloud API), Claude, Whisper (opcional), SMTP (opcional) e a equipe do consultório.

Fonte: [`fonte/01-contexto.mmd`](fonte/01-contexto.mmd)

```mermaid
flowchart LR
    paciente(["👤 Paciente<br/>(WhatsApp)"])
    equipe(["🦷 Equipe do consultório<br/>(WhatsApp, e-mail, API admin)"])

    subgraph meta["Meta"]
        cloud["WhatsApp Business Platform<br/>Cloud API (Graph API v24.0)"]
    end

    subgraph servidor["Servidor do consultório (Docker)"]
        agente["Agente WhatsApp Odonto<br/>Java 21 · Spring Boot 4 · Spring AI 2"]
        banco[("PostgreSQL 18<br/>pacientes, prontuários,<br/>agenda, conversas")]
    end

    claude["Anthropic Claude<br/>(classificar queixa e<br/>interpretar data/hora)"]
    whisper["OpenAI Whisper<br/>(opcional: transcrever áudio)"]
    smtp["Servidor SMTP<br/>(opcional)"]

    paciente <-->|mensagens| cloud
    cloud -->|webhook assinado<br/>HMAC-SHA256| agente
    agente -->|respostas e avisos| cloud
    cloud -->|aviso de agendamento| equipe
    agente <--> banco
    agente -->|texto da queixa / data| claude
    agente -->|áudio| whisper
    agente -->|aviso por e-mail| smtp
    smtp --> equipe
    equipe -->|Bearer ADMIN_TOKEN| agente
```

## 2. Camadas da Clean Architecture

Regra de dependência: infraestrutura → adaptador → aplicação → domínio. Verificada automaticamente pelo `ArquiteturaTest` (ArchUnit).

Fonte: [`fonte/02-camadas.mmd`](fonte/02-camadas.mmd)

```mermaid
flowchart TB
    subgraph infra["infraestrutura — frameworks e composição"]
        direction LR
        app["AgenteOdontoApplication"]
        props["OdontoProperties"]
        cfg["CasosDeUsoConfig · ConfiguracoesConfig · IaConfig"]
    end
    subgraph adaptador["adaptador — entrada e saída"]
        direction LR
        entrada["entrada<br/>webhook · web · evento · agendamento"]
        saida["saída<br/>persistencia · whatsapp · ia · transcricao<br/>notificacao · plataforma"]
    end
    subgraph aplicacao["aplicacao — casos de uso e portas"]
        direction LR
        pe["porta.entrada<br/>*UseCase, comandos, visões"]
        cu["casodeuso<br/>*Service"]
        ps["porta.saida<br/>repositórios, enviador, IA, relógio"]
    end
    subgraph dominio["dominio — regras de negócio puras (só java.*)"]
        direction LR
        modelo["modelo<br/>Paciente · Prontuario · Consulta · Conversa"]
        servico["servico<br/>Agenda · Classificador · Validador"]
    end

    infra --> adaptador
    adaptador --> aplicacao
    aplicacao --> dominio
    entrada -.chama.-> pe
    cu -.implementa.-> pe
    cu -.usa.-> ps
    saida -.implementa.-> ps

    classDef camada fill:#f7f9fc,stroke:#4a6fa5,stroke-width:1px;
    class infra,adaptador,aplicacao,dominio camada;
```

## 3. Componentes, portas e adaptadores

Como os adaptadores de entrada chamam as portas de entrada (casos de uso) e como os adaptadores de saída implementam as portas de saída.

Fonte: [`fonte/03-componentes.mmd`](fonte/03-componentes.mmd)

```mermaid
flowchart LR
    subgraph entrada["Adaptadores de entrada"]
        wh["WebhookController<br/>+ AssinaturaWebhookVerifier<br/>+ ConversorWebhook"]
        ouv["OuvinteEventos<br/>(@Async, trava por conversa)"]
        tar["TarefasAgendadas<br/>(expiração e expurgo)"]
        api["AgendaController · PacienteController<br/>CatalogoController (AdminTokenFilter)"]
    end

    subgraph app["Aplicação"]
        rec["ReceberMensagensUseCase"]
        cond["ConduzirConversaUseCase<br/>ProcessadorComandosConversa<br/>RespondedorPaciente"]
        ident["IdentificarPaciente · CadastrarPaciente"]
        queixa["RegistrarQueixaUseCase"]
        agendar["AgendarConsultaUseCase"]
        notif["NotificarConsultorioUseCase"]
        transc["TranscreverAudioUseCase"]
        adm["GerenciarAgenda · ConsultarPacientes<br/>ReclassificarQueixa · ConsultarCatalogo"]
        manut["ExpirarConversas · ExpurgarMensagens"]
    end

    subgraph saida["Adaptadores de saída"]
        jpa["*JpaAdapter<br/>(PostgreSQL, AES-GCM, HMAC)"]
        wa["WhatsAppCloudApiAdapter"]
        ia["ClassificadorProblemaClaudeAdapter<br/>InterpretadorDataHoraClaudeAdapter"]
        wsp["WhisperTranscritorAdapter"]
        canais["Canais: WhatsApp · E-mail · Log"]
        pub["SpringPublicadorEventos"]
    end

    wh --> rec --> pub -.MensagemRecebidaEvento.-> ouv
    ouv --> cond
    cond --> ident & queixa & agendar & transc & notif
    agendar -.ConsultaAgendadaEvento.-> ouv
    ouv --> notif
    tar --> manut
    api --> adm
    queixa --> ia
    cond --> ia
    transc --> wa & wsp
    cond --> wa
    notif --> canais
    ident & queixa & agendar & adm & manut & rec & cond --> jpa
```

## 4. Máquina de estados da conversa

Etapas fixas da `Conversa` (workflow agêntico previsível e auditável).

Fonte: [`fonte/04-maquina-estados-conversa.mmd`](fonte/04-maquina-estados-conversa.mmd)

```mermaid
stateDiagram-v2
    [*] --> AGUARDANDO_RG: primeira mensagem<br/>(BOAS_VINDAS)

    AGUARDANDO_RG --> AGUARDANDO_NOME: RG não cadastrado
    AGUARDANDO_RG --> AGUARDANDO_PROBLEMA: paciente encontrado
    AGUARDANDO_RG --> AGUARDANDO_CONFIRMACAO_NOVA_CONSULTA: já tem consulta futura

    AGUARDANDO_NOME --> AGUARDANDO_DATA_NASCIMENTO
    AGUARDANDO_DATA_NASCIMENTO --> AGUARDANDO_RESPONSAVEL: menor de 18 anos
    AGUARDANDO_DATA_NASCIMENTO --> AGUARDANDO_CONSENTIMENTO: maior de idade
    AGUARDANDO_RESPONSAVEL --> AGUARDANDO_CONSENTIMENTO
    AGUARDANDO_CONSENTIMENTO --> AGUARDANDO_PROBLEMA: SIM → cadastro + prontuário
    AGUARDANDO_CONSENTIMENTO --> ENCERRADA_SEM_CONSENTIMENTO: NÃO → dados descartados,<br/>equipe avisada

    AGUARDANDO_CONFIRMACAO_NOVA_CONSULTA --> AGUARDANDO_PROBLEMA: SIM
    AGUARDANDO_CONFIRMACAO_NOVA_CONSULTA --> CONCLUIDA: NÃO (ENCERRAMENTO)

    AGUARDANDO_PROBLEMA --> AGUARDANDO_DATA_HORA: queixa classificada<br/>e registrada no prontuário
    AGUARDANDO_PROBLEMA --> AGUARDANDO_CONFIRMACAO_SUGESTAO: EMERGÊNCIA → equipe avisada,<br/>oferece 1º horário livre

    AGUARDANDO_DATA_HORA --> CONCLUIDA: horário livre → AGENDADO
    AGUARDANDO_DATA_HORA --> AGUARDANDO_CONFIRMACAO_SUGESTAO: ocupado ou impreciso →<br/>sugere o mais próximo
    AGUARDANDO_DATA_HORA --> ENCAMINHADA_ATENDIMENTO_HUMANO: sem horário livre

    AGUARDANDO_CONFIRMACAO_SUGESTAO --> CONCLUIDA: SIM → AGENDADO
    AGUARDANDO_CONFIRMACAO_SUGESTAO --> AGUARDANDO_DATA_HORA: NÃO
    AGUARDANDO_CONFIRMACAO_SUGESTAO --> AGUARDANDO_CONFIRMACAO_SUGESTAO: outro dia/horário

    CONCLUIDA --> AGUARDANDO_RG: nova mensagem reinicia
    ENCERRADA_SEM_CONSENTIMENTO --> AGUARDANDO_RG: após expirar
    ENCAMINHADA_ATENDIMENTO_HUMANO --> AGUARDANDO_RG: após expirar

    note right of ENCAMINHADA_ATENDIMENTO_HUMANO
        Também alcançado de qualquer etapa
        após N respostas inválidas seguidas
        (NAO_ENTENDI + dica da etapa).
        O agente fica em silêncio até expirar.
    end note
```

## 5. Sequência: primeiro contato e cadastro

Identificação pelo RG + UF, cadastro, consentimento LGPD e abertura do prontuário.

Fonte: [`fonte/05-sequencia-primeiro-contato.mmd`](fonte/05-sequencia-primeiro-contato.mmd)

```mermaid
sequenceDiagram
    autonumber
    actor P as Paciente
    participant W as WhatsApp Cloud API
    participant A as Agente (Conversa)
    participant DB as PostgreSQL

    P->>W: "Oi"
    W->>A: webhook (mensagem)
    A-->>P: BOAS_VINDAS: informe RG e UF
    P->>A: "12.345.678-9 SP"
    A->>DB: busca paciente pelo HMAC do RG
    DB-->>A: não encontrado
    A-->>P: PEDIR_NOME
    P->>A: "Maria Aparecida da Silva"
    A-->>P: PEDIR_DATA_NASCIMENTO
    P->>A: "25/03/1990"
    alt menor de 18 anos
        A-->>P: PEDIR_RESPONSAVEL
        P->>A: nome do responsável
    end
    A-->>P: PEDIR_CONSENTIMENTO (LGPD)
    P->>A: "sim"
    A->>DB: grava Paciente (dados cifrados) + Prontuário PR-000123
    A-->>P: CADASTRO_CONCLUIDO + pede a queixa
```

## 6. Sequência: queixa e agendamento

Classificação da queixa (IA com plano B por palavras-chave), interpretação de data/hora e sugestão do horário livre mais próximo.

Fonte: [`fonte/06-sequencia-queixa-agendamento.mmd`](fonte/06-sequencia-queixa-agendamento.mmd)

```mermaid
sequenceDiagram
    autonumber
    actor P as Paciente
    participant A as Agente
    participant C as Claude (Spring AI)
    participant AG as Agenda (domínio)
    participant DB as PostgreSQL
    actor E as Consultório

    P->>A: "Dor forte no dente ao tomar gelado"
    A->>A: DetectorSinaisAlarme (palavras de emergência)
    A->>C: classificar no catálogo (JSON estruturado)
    alt IA indisponível ou confiança baixa
        A->>A: ClassificadorPorPalavraChave
    end
    A->>DB: QueixaRelatada no prontuário
    A-->>P: PEDIR_DATA_HORA
    P->>A: "sexta às 10h"
    A->>A: InterpretadorDataHoraDeterministico
    opt texto não reconhecido
        A->>C: interpretar data/hora (JSON estruturado)
    end
    A->>AG: resolver(preferência)
    alt horário livre
        A->>DB: grava Consulta (índice único evita dupla reserva)
        A-->>P: AGENDADO (data, hora, endereço)
        A-)E: aviso: paciente, data/hora, problema, prontuário
    else ocupado ou indisponível
        AG-->>A: horário livre mais próximo
        A-->>P: SUGERIR_HORARIO ("Posso agendar?")
        P->>A: "sim"
        A->>DB: grava Consulta
        A-->>P: AGENDADO
        A-)E: aviso de agendamento
    end
```

## 7. Sequência: webhook e processamento assíncrono

Assinatura, deduplicação, evento após o commit, processamento assíncrono com trava por conversa.

Fonte: [`fonte/07-sequencia-webhook.mmd`](fonte/07-sequencia-webhook.mmd)

```mermaid
sequenceDiagram
    autonumber
    participant M as Meta (Cloud API)
    participant WC as WebhookController
    participant V as AssinaturaWebhookVerifier
    participant R as ReceberMensagensService
    participant DB as PostgreSQL
    participant O as OuvinteEventos (@Async)
    participant CC as ConduzirConversaService

    M->>WC: POST /webhook/whatsapp (corpo + X-Hub-Signature-256)
    WC->>V: HMAC-SHA256 com App Secret (tempo constante)
    alt assinatura inválida
        WC-->>M: 401
    else válida
        WC->>R: comandos convertidos (texto, áudio, botão)
        R->>DB: grava Mensagem (wa_message_id único → ignora duplicadas)
        R-)O: MensagemRecebidaEvento (após o commit)
        WC-->>M: 200 OK (rápido, sem esperar a IA)
        O->>O: trava da conversa (uma mensagem por vez por número)
        O->>CC: conduzir(mensagemId)
        CC->>DB: carrega Conversa (dados cifrados)
        CC->>CC: Conversa.registrarResposta → AcaoConversa
        loop enquanto houver comando
            CC->>CC: ProcessadorComandosConversa executa e devolve resultado à Conversa
        end
        CC->>DB: salva Conversa
        CC->>M: envia respostas (Graph API /messages)
    end
```

## 8. Classes do domínio

Modelo rico: as regras ficam nos objetos (Paciente, Prontuario, Consulta, Conversa) e no serviço de domínio Agenda.

Fonte: [`fonte/08-classes-dominio.mmd`](fonte/08-classes-dominio.mmd)

```mermaid
classDiagram
    direction LR
    class Paciente {
        -Long id
        -DocumentoRg rg
        -String nomeCompleto
        -LocalDate dataNascimento
        -Optional~String~ responsavelLegal
        -Set~WaId~ telefones
        -boolean consentimentoLgpd
        +cadastrar(...)$ Paciente
        +vincularTelefone(WaId)
        +idade(LocalDate) int
    }
    class Prontuario {
        -String numero
        -List~QueixaRelatada~ queixas
        +abrir(pacienteId, seq, agora)$ Prontuario
        +registrarQueixa(texto, ClassificacaoQueixa, agora)
        +reclassificarQueixa(id, ProblemaOdontologico)
        +vincularConsulta(queixaId, consultaId)
    }
    class QueixaRelatada {
        -String textoOriginal
        -ProblemaOdontologico problema
        -double confianca
        -OrigemClassificacao origem
        -Optional~Long~ consultaId
    }
    class ProblemaOdontologico {
        <<record>>
        String codigo
        String nome
        String especialidade
        Prioridade prioridade
        Optional~String~ cid10
        List~String~ palavrasChave
    }
    class Consulta {
        -Horario horario
        -StatusConsulta status
        +agendar(...)$ Consulta
        +cancelar()
        +marcarRealizada(agora)
        +marcarFalta(agora)
    }
    class Conversa {
        -WaId waId
        -EtapaConversa etapa
        -DadosConversa dados
        -int tentativasInvalidas
        +registrarResposta(texto, agora, hoje, regras) AcaoConversa
        +pacienteEncontrado(...) AcaoConversa
        +queixaRegistrada(...) AcaoConversa
        +disponibilidadeVerificada(...) AcaoConversa
        +consultaAgendada(Horario) AcaoConversa
    }
    class AcaoConversa {
        <<record>>
        List~RespostaAgente~ respostas
        Optional~AlertaConversa~ alerta
        Optional~ComandoConversa~ comando
    }
    class Agenda {
        <<service>>
        +resolver(PreferenciaHorario, agora) Disponibilidade
        +primeiroLivre(agora) Disponibilidade
        +horariosLivres(de, ate, agora)
    }
    class DocumentoRg {
        <<value object>>
        String numero
        Uf uf
    }
    class WaId {
        <<value object>>
        String valor
    }
    class Horario {
        <<value object>>
        LocalDateTime inicio
        LocalDateTime fim
    }
    class Prioridade {
        <<enumeration>>
        EMERGENCIA
        URGENCIA
        ROTINA
    }
    class EtapaConversa {
        <<enumeration>>
        AGUARDANDO_RG
        AGUARDANDO_PROBLEMA
        AGUARDANDO_DATA_HORA
        CONCLUIDA
    }
    class StatusConsulta {
        <<enumeration>>
        AGENDADA
        CANCELADA
        REALIZADA
        FALTOU
    }

    Paciente "1" -- "1" Prontuario
    Prontuario "1" *-- "*" QueixaRelatada
    QueixaRelatada --> ProblemaOdontologico
    Consulta --> Paciente
    Consulta --> QueixaRelatada
    Consulta *-- Horario
    Consulta --> StatusConsulta
    Paciente *-- DocumentoRg
    Paciente *-- WaId
    Conversa *-- WaId
    Conversa --> EtapaConversa
    Conversa ..> AcaoConversa : devolve
    ProblemaOdontologico --> Prioridade
    Agenda ..> Horario
```

## 9. Modelo de dados (PostgreSQL)

Tabelas criadas pelo Flyway (`V1__esquema_inicial.sql`). Colunas `*_cifrado` usam AES-256-GCM; `rg_hash` é HMAC-SHA256.

Fonte: [`fonte/09-modelo-dados.mmd`](fonte/09-modelo-dados.mmd)

```mermaid
erDiagram
    PACIENTE ||--|{ PACIENTE_TELEFONE : "tem"
    PACIENTE ||--|| PRONTUARIO : "possui"
    PRONTUARIO ||--o{ QUEIXA_RELATADA : "registra"
    PROBLEMA_ODONTOLOGICO ||--o{ QUEIXA_RELATADA : "classifica"
    PACIENTE ||--o{ CONSULTA : "agenda"
    QUEIXA_RELATADA |o--o| CONSULTA : "motiva"

    PACIENTE {
        bigint id PK
        varchar rg_hash UK "HMAC-SHA256 (busca)"
        text rg_cifrado "AES-256-GCM"
        text nome_cifrado "AES-256-GCM"
        text data_nascimento_cifrada "AES-256-GCM"
        text responsavel_cifrado "AES-256-GCM"
        boolean consentimento_lgpd
        timestamptz data_consentimento
        timestamptz data_cadastro
    }
    PACIENTE_TELEFONE {
        bigint paciente_id FK
        varchar wa_id
    }
    PRONTUARIO {
        bigint id PK
        bigint paciente_id FK,UK
        varchar numero UK "PR-000123"
        timestamptz data_abertura
    }
    QUEIXA_RELATADA {
        bigint id PK
        bigint prontuario_id FK
        text texto_cifrado "AES-256-GCM"
        varchar problema_codigo FK
        varchar prioridade
        float confianca
        varchar origem "IA, PALAVRA_CHAVE, MANUAL"
        timestamptz data_registro
        bigint consulta_id FK
    }
    PROBLEMA_ODONTOLOGICO {
        varchar codigo PK
        varchar nome
        text descricao
        varchar especialidade
        varchar prioridade
        varchar cid10
        text palavras_chave
        boolean ativo
    }
    CONSULTA {
        bigint id PK
        bigint paciente_id FK
        bigint queixa_id FK
        timestamptz inicio "único entre AGENDADAS"
        timestamptz fim
        varchar status
        timestamptz criada_em
    }
    BLOQUEIO_AGENDA {
        bigint id PK
        timestamptz inicio
        timestamptz fim
        varchar motivo
    }
    CONVERSA {
        bigint id PK
        varchar wa_id UK
        varchar etapa
        text dados_cifrados "JSON cifrado"
        int tentativas_invalidas
        timestamptz ultima_interacao
        timestamptz iniciada_em
    }
    MENSAGEM {
        bigint id PK
        varchar wa_message_id UK "deduplicação"
        varchar wa_id
        varchar direcao
        varchar tipo
        text texto_cifrado
        varchar midia_id
        boolean automatica
        timestamptz criada_em
    }
```

## 10. Implantação

Um único contêiner com a aplicação e o PostgreSQL, volume persistente e proxy reverso HTTPS na frente.

Fonte: [`fonte/10-implantacao.mmd`](fonte/10-implantacao.mmd)

```mermaid
flowchart TB
    internet(("Internet"))
    subgraph host["Servidor (Linux com Docker)"]
        proxy["Proxy reverso HTTPS<br/>(Caddy, Nginx ou Cloudflare Tunnel)"]
        subgraph cont["Contêiner agent-whatsapp-odonto"]
            ep["entrypoint.sh<br/>(supervisiona os 2 processos)"]
            java["Java 21 · app.jar<br/>porta 8080 · usuário odonto"]
            pg["PostgreSQL 18<br/>só 127.0.0.1:5432 · usuário postgres"]
            ep --> pg
            ep --> java
            java --> pg
        end
        vol[("Volume Docker<br/>agent-whatsapp-odonto-dados<br/>/var/lib/postgresql")]
        env[/".env (segredos, chmod 600)"/]
        bkp[/"./backups (pg_dump -Fc)"/]
        sh["odonto.sh"]
    end

    internet -->|"443: /webhook/whatsapp"| proxy --> java
    pg --- vol
    env -.-> cont
    sh -->|"docker compose"| cont
    sh -->|backup / restaurar| bkp
```
