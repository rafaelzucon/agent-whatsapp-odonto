package br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb;

import br.com.consultorio.odonto.adaptador.saida.persistencia.FusoPersistencia;
import br.com.consultorio.odonto.adaptador.saida.persistencia.catalogo.CatalogoProblemasClasspathAdapter;
import br.com.consultorio.odonto.adaptador.saida.persistencia.cripto.ConfiguracaoCriptografia;
import br.com.consultorio.odonto.adaptador.saida.persistencia.cripto.CriptografiaService;
import br.com.consultorio.odonto.adaptador.saida.persistencia.cripto.HmacCifradorDocumento;
import br.com.consultorio.odonto.aplicacao.excecao.MensagemDuplicadaException;
import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import br.com.consultorio.odonto.dominio.excecao.HorarioIndisponivelException;
import br.com.consultorio.odonto.dominio.modelo.BloqueioAgenda;
import br.com.consultorio.odonto.dominio.modelo.ClassificacaoQueixa;
import br.com.consultorio.odonto.dominio.modelo.Consulta;
import br.com.consultorio.odonto.dominio.modelo.Conversa;
import br.com.consultorio.odonto.dominio.modelo.DocumentoRg;
import br.com.consultorio.odonto.dominio.modelo.EtapaConversa;
import br.com.consultorio.odonto.dominio.modelo.Horario;
import br.com.consultorio.odonto.dominio.modelo.Mensagem;
import br.com.consultorio.odonto.dominio.modelo.OrigemClassificacao;
import br.com.consultorio.odonto.dominio.modelo.Paciente;
import br.com.consultorio.odonto.dominio.modelo.Prioridade;
import br.com.consultorio.odonto.dominio.modelo.Prontuario;
import br.com.consultorio.odonto.dominio.modelo.StatusConsulta;
import br.com.consultorio.odonto.dominio.modelo.TipoConteudo;
import br.com.consultorio.odonto.dominio.modelo.WaId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Adaptadores DynamoDB contra o DynamoDB Local (contêiner). Cobrem as garantias que no PostgreSQL vêm de restrições
 * do banco: unicidade de horário, de RG e de mensagem, atomicidade das transações e intervalos semiabertos.
 */
@Testcontainers(disabledWithoutDocker = true)
class DynamoDbAdaptersTest {

    private static final Instant AGORA = Instant.parse("2026-11-16T12:00:00Z");
    private static final LocalDateTime SEGUNDA_9H = LocalDateTime.of(2026, 11, 16, 9, 0);

    @Container
    static final GenericContainer<?> DYNAMODB = new GenericContainer<>("amazon/dynamodb-local:3.1.0")
            .withCommand("-jar DynamoDBLocal.jar -inMemory -sharedDb").withExposedPorts(8000);

    private static DynamoDbClient cliente;

    private TabelaDynamoDb tabela;
    private TransactionTemplate transacao;
    private PacienteDynamoDbAdapter pacientes;
    private ProntuarioDynamoDbAdapter prontuarios;
    private ConsultaDynamoDbAdapter consultas;
    private BloqueioAgendaDynamoDbAdapter bloqueios;
    private ConversaDynamoDbAdapter conversas;
    private MensagemDynamoDbAdapter mensagens;
    private final CatalogoProblemasClasspathAdapter catalogo = new CatalogoProblemasClasspathAdapter();

    @BeforeAll
    static void conectar() {
        cliente = DynamoDbClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .endpointOverride(URI.create("http://" + DYNAMODB.getHost() + ":" + DYNAMODB.getMappedPort(8000)))
                .region(Region.SA_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("teste", "teste")))
                .build();
    }

    @BeforeEach
    void criarTabela() {
        String nome = "odonto-" + UUID.randomUUID();
        cliente.createTable(r -> r.tableName(nome).billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(atributo("pk"), atributo("sk"), atributo("gsi1pk"), atributo("gsi1sk"),
                        atributo("gsi2pk"), atributo("gsi2sk"))
                .keySchema(chave("pk", KeyType.HASH), chave("sk", KeyType.RANGE))
                .globalSecondaryIndexes(indice(TabelaDynamoDb.Indice.GSI1), indice(TabelaDynamoDb.Indice.GSI2)));
        tabela = new TabelaDynamoDb(cliente, nome);
        transacao = new TransactionTemplate(new GerenciadorTransacaoDynamoDb(tabela));
        var configuracao = new ConfiguracaoCriptografia("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=");
        var cripto = new CriptografiaService(configuracao);
        var fuso = new FusoPersistencia(new Relogio() {
            @Override
            public Instant agora() {
                return AGORA;
            }

            @Override
            public ZoneId fuso() {
                return ZoneId.of("America/Sao_Paulo");
            }
        });
        pacientes = new PacienteDynamoDbAdapter(tabela, new HmacCifradorDocumento(configuracao), cripto);
        prontuarios = new ProntuarioDynamoDbAdapter(tabela, catalogo, cripto);
        consultas = new ConsultaDynamoDbAdapter(tabela, fuso);
        bloqueios = new BloqueioAgendaDynamoDbAdapter(tabela, fuso);
        conversas = new ConversaDynamoDbAdapter(tabela, cripto, JsonMapper.builder().build());
        mensagens = new MensagemDynamoDbAdapter(tabela, cripto);
    }

    @Test
    void pacienteDeIdaEVoltaComDadosCifradosEUnicidadeDoRg() {
        Paciente salvo = pacientes.salvar(paciente("123456789"));

        assertThat(salvo.id()).isPresent();
        Paciente lido = pacientes.buscarPorId(salvo.id().orElseThrow()).orElseThrow();
        assertThat(lido.nomeCompleto()).isEqualTo("Maria da Silva");
        assertThat(lido.rg()).isEqualTo(DocumentoRg.de("123456789", "SP"));
        assertThat(lido.telefones()).containsExactly(new WaId("5511999990000"));
        assertThat(pacientes.buscarPorRgHash(new HmacCifradorDocumento(new ConfiguracaoCriptografia(
                "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=", "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA="))
                .hash(lido.rg()))).isPresent();
        assertThat(pacientes.listarTodos()).hasSize(1);
        String nomeGravado = tabela.buscar(new Chave("PACIENTE#" + lido.id().orElseThrow(), "PACIENTE"))
                .orElseThrow().get("nome").s();
        assertThat(nomeGravado).doesNotContain("Maria");

        assertThatThrownBy(() -> pacientes.salvar(paciente("123456789"))).isInstanceOf(IllegalStateException.class);
        assertThat(pacientes.listarTodos()).hasSize(1);
    }

    @Test
    void prontuarioGuardaQueixasEBuscaPelaQueixa() {
        long pacienteId = pacientes.salvar(paciente("123456789")).id().orElseThrow();
        Prontuario prontuario = Prontuario.abrir(pacienteId, prontuarios.proximoSequencial(), AGORA);
        prontuario.registrarQueixa("Dor forte no dente", new ClassificacaoQueixa(
                catalogo.buscarPorCodigo("AVALIACAO_GERAL").orElseThrow(), 0.9, OrigemClassificacao.IA,
                Prioridade.URGENCIA), AGORA);

        Prontuario salvo = prontuarios.salvar(prontuario);
        long queixaId = salvo.queixas().getFirst().id().orElseThrow();
        salvo.vincularConsulta(queixaId, 42L);
        prontuarios.salvar(salvo);

        Prontuario lido = prontuarios.buscarPorQueixaId(queixaId).orElseThrow();
        assertThat(lido.numero()).isEqualTo(salvo.numero());
        assertThat(lido.queixas()).singleElement().satisfies(q -> {
            assertThat(q.textoOriginal()).isEqualTo("Dor forte no dente");
            assertThat(q.prioridade()).isEqualTo(Prioridade.URGENCIA);
            assertThat(q.consultaId()).contains(42L);
        });
        assertThat(prontuarios.buscarPorPacienteId(pacienteId)).isPresent();
        assertThat(prontuarios.proximoSequencial()).isEqualTo(2);
    }

    @Test
    void mesmoHorarioSoAceitaUmaConsultaAgendadaECancelarLiberaOHorario() {
        Horario horario = Horario.de(SEGUNDA_9H, Duration.ofMinutes(30));
        Consulta primeira = consultas.salvar(Consulta.agendar(1L, Optional.empty(), horario, AGORA));

        assertThatThrownBy(() -> consultas.salvar(Consulta.agendar(2L, Optional.empty(), horario, AGORA)))
                .isInstanceOf(HorarioIndisponivelException.class);

        primeira.cancelar();
        consultas.salvar(primeira);
        Consulta segunda = consultas.salvar(Consulta.agendar(2L, Optional.of(7L), horario, AGORA));

        assertThat(consultas.listarEntre(SEGUNDA_9H, SEGUNDA_9H.plusHours(1))).hasSize(2);
        assertThat(consultas.listarAgendadasEntre(SEGUNDA_9H, SEGUNDA_9H.plusHours(1)))
                .extracting(c -> c.id().orElseThrow()).containsExactly(segunda.id().orElseThrow());
        assertThat(consultas.buscarPorId(segunda.id().orElseThrow()).orElseThrow().queixaId()).contains(7L);
    }

    @Test
    void listagensDeConsultasUsamIntervaloSemiabertoEOrdemDeInicio() {
        consultas.salvar(Consulta.agendar(1L, Optional.empty(), Horario.de(SEGUNDA_9H.plusHours(1),
                Duration.ofMinutes(30)), AGORA));
        consultas.salvar(Consulta.agendar(1L, Optional.empty(), Horario.de(SEGUNDA_9H, Duration.ofMinutes(30)),
                AGORA));

        assertThat(consultas.listarEntre(SEGUNDA_9H, SEGUNDA_9H.plusHours(1)))
                .extracting(c -> c.horario().inicio()).containsExactly(SEGUNDA_9H);
        assertThat(consultas.listarPorPaciente(1L)).extracting(c -> c.horario().inicio())
                .containsExactly(SEGUNDA_9H, SEGUNDA_9H.plusHours(1));
        assertThat(consultas.proximaAgendadaDoPaciente(1L, SEGUNDA_9H)).map(c -> c.horario().inicio())
                .contains(SEGUNDA_9H.plusHours(1));
        assertThat(consultas.proximaAgendadaDoPaciente(2L, SEGUNDA_9H)).isEmpty();
    }

    @Test
    void bloqueiosSobrepostosERemocao() {
        BloqueioAgenda ferias = bloqueios.salvar(BloqueioAgenda.novo(Horario.de(SEGUNDA_9H, Duration.ofDays(2)),
                "Férias"));

        assertThat(bloqueios.listarEntre(SEGUNDA_9H.plusDays(1), SEGUNDA_9H.plusDays(1).plusHours(1)))
                .extracting(BloqueioAgenda::motivo).containsExactly("Férias");
        assertThat(bloqueios.listarEntre(SEGUNDA_9H.plusDays(2), SEGUNDA_9H.plusDays(3))).isEmpty();
        assertThat(bloqueios.listarEntre(SEGUNDA_9H.minusHours(1), SEGUNDA_9H)).isEmpty();
        assertThat(bloqueios.remover(ferias.id().orElseThrow())).isTrue();
        assertThat(bloqueios.remover(ferias.id().orElseThrow())).isFalse();
    }

    @Test
    void mensagemDuplicadaERetencao() {
        mensagens.salvar(Mensagem.recebida("wamid.1", new WaId("5511999990000"), TipoConteudo.TEXTO,
                Optional.of("Oi"), Optional.empty(), AGORA.minus(Duration.ofDays(400))));
        Mensagem recente = mensagens.salvar(Mensagem.recebida("wamid.2", new WaId("5511999990000"),
                TipoConteudo.TEXTO, Optional.of("Olá"), Optional.empty(), AGORA));

        assertThatThrownBy(() -> mensagens.salvar(Mensagem.recebida("wamid.2", new WaId("5511999990000"),
                TipoConteudo.TEXTO, Optional.of("Olá"), Optional.empty(), AGORA)))
                .isInstanceOf(MensagemDuplicadaException.class);
        assertThat(mensagens.buscarPorId(recente.id().orElseThrow()).orElseThrow().texto()).contains("Olá");

        assertThat(mensagens.removerAnterioresA(AGORA.minus(Duration.ofDays(365)))).isEqualTo(1);
        assertThat(mensagens.existePorWaMessageId("wamid.1")).isFalse();
        assertThat(mensagens.existePorWaMessageId("wamid.2")).isTrue();
    }

    @Test
    void conversaDeIdaEVoltaEExpiracaoPorEtapa() {
        WaId waId = new WaId("5511999990000");
        Conversa salva = conversas.salvar(Conversa.nova(waId, AGORA.minus(Duration.ofHours(3))));
        Conversa substituta = conversas.salvar(Conversa.nova(waId, AGORA.minus(Duration.ofHours(3))));

        assertThat(substituta.id()).isEqualTo(salva.id());
        assertThat(conversas.buscarPorWaId(waId).orElseThrow().etapa()).isEqualTo(EtapaConversa.AGUARDANDO_RG);
        assertThat(conversas.removerInativasDesde(AGORA.minus(Duration.ofHours(2)), Set.of())).isZero();
        assertThat(conversas.removerInativasDesde(AGORA.minus(Duration.ofHours(2)),
                Set.of(EtapaConversa.AGUARDANDO_PROBLEMA))).isZero();
        assertThat(conversas.removerInativasDesde(AGORA.minus(Duration.ofHours(2)),
                Set.of(EtapaConversa.AGUARDANDO_RG))).isEqualTo(1);
        assertThat(conversas.buscarPorWaId(waId)).isEmpty();
    }

    @Test
    void falhaDentroDaTransacaoNaoGravaNada() {
        assertThatThrownBy(() -> transacao.executeWithoutResult(status -> {
            pacientes.salvar(paciente("123456789"));
            throw new IllegalArgumentException("falhou");
        })).isInstanceOf(IllegalArgumentException.class);

        assertThat(pacientes.listarTodos()).isEmpty();
    }

    @Test
    void conflitoNoCommitDesfazTudoELancaAExcecaoDoDominio() {
        Horario horario = Horario.de(SEGUNDA_9H, Duration.ofMinutes(30));
        consultas.salvar(Consulta.agendar(1L, Optional.empty(), horario, AGORA));
        List<String> depoisDoCommit = new ArrayList<>();

        assertThatThrownBy(() -> transacao.executeWithoutResult(status -> {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    depoisDoCommit.add(TransactionPhase.AFTER_COMMIT.name());
                }
            });
            pacientes.salvar(paciente("987654321"));
            consultas.salvar(Consulta.agendar(2L, Optional.empty(), horario, AGORA));
        })).isInstanceOf(HorarioIndisponivelException.class);

        assertThat(pacientes.listarTodos()).isEmpty();
        assertThat(depoisDoCommit).isEmpty();

        transacao.executeWithoutResult(status -> {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    depoisDoCommit.add(TransactionPhase.AFTER_COMMIT.name());
                }
            });
            Paciente salvo = pacientes.salvar(paciente("987654321"));
            assertThat(pacientes.buscarPorId(salvo.id().orElseThrow())).isPresent();
        });
        assertThat(pacientes.listarTodos()).hasSize(1);
        assertThat(depoisDoCommit).containsExactly("AFTER_COMMIT");
    }

    private static Paciente paciente(String rg) {
        return Paciente.cadastrar(DocumentoRg.de(rg, "SP"), "Maria da Silva", LocalDate.of(1990, 5, 20),
                Optional.empty(), new WaId("5511999990000"), true, AGORA, LocalDate.of(2026, 11, 16));
    }

    private static AttributeDefinition atributo(String nome) {
        return AttributeDefinition.builder().attributeName(nome).attributeType(ScalarAttributeType.S).build();
    }

    private static KeySchemaElement chave(String nome, KeyType tipo) {
        return KeySchemaElement.builder().attributeName(nome).keyType(tipo).build();
    }

    private static GlobalSecondaryIndex indice(TabelaDynamoDb.Indice indice) {
        return GlobalSecondaryIndex.builder().indexName(indice.nome())
                .keySchema(chave(indice.particao(), KeyType.HASH), chave(indice.ordenacao(), KeyType.RANGE))
                .projection(p -> p.projectionType(ProjectionType.ALL)).build();
    }
}
