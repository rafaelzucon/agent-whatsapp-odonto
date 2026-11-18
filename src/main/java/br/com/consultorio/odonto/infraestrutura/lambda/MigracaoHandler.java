package br.com.consultorio.odonto.infraestrutura.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Aplica as migrações do Flyway no banco. No Lambda as funções da aplicação sobem com o Flyway desligado (a
 * migração não pode acontecer dentro do snapshot do SnapStart nem em cada partida a frio), então ela roda
 * como passo da implantação: um recurso personalizado do CloudFormation chama esta função a cada nova versão
 * e só depois a fila é ligada à função da conversa. Também pode ser invocada manualmente (sem
 * {@code ResponseURL}).
 */
public class MigracaoHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    static final String ID_FISICO = "odonto-migracao-banco";
    private static final Logger LOG = LoggerFactory.getLogger(MigracaoHandler.class);
    private static final long MARGEM_MS = 10_000;

    private final Supplier<Map<String, Object>> migracao;
    private final HttpClient http;
    private final JsonMapper json = JsonMapper.builder().build();

    public MigracaoHandler() {
        this(migracaoFlyway(SegredosAws.resolvedor(SegredosAws.ler())),
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    MigracaoHandler(Supplier<Map<String, Object>> migracao, HttpClient http) {
        this.migracao = Objects.requireNonNull(migracao);
        this.http = Objects.requireNonNull(http);
    }

    static Supplier<Map<String, Object>> migracaoFlyway(UnaryOperator<String> configuracao) {
        return () -> {
            String url = Objects.requireNonNull(configuracao.apply("DB_URL"), "DB_URL não definido");
            MigrateResult resultado = Flyway.configure()
                    .dataSource(url, configuracao.apply("DB_USUARIO"), configuracao.apply("DB_SENHA"))
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();
            Map<String, Object> dados = new LinkedHashMap<>();
            dados.put("MigracoesAplicadas", resultado.migrationsExecuted);
            dados.put("VersaoEsquema", Objects.toString(resultado.targetSchemaVersion,
                    Objects.toString(resultado.initialSchemaVersion, "")));
            return dados;
        };
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> evento, Context contexto) {
        Map<String, Object> pedido = evento == null ? Map.of() : evento;
        String tipo = texto(pedido.get("RequestType"));
        String urlResposta = texto(pedido.get("ResponseURL"));
        Map<String, Object> dados = Map.of();
        String falha = null;
        if ("Delete".equals(tipo)) {
            LOG.info("Remoção da pilha: o banco e os dados são preservados.");
        } else {
            try {
                dados = executarComLimite(limite(contexto));
                LOG.info("Migração concluída: {}", dados);
            } catch (RuntimeException e) {
                LOG.error("Falha na migração do banco.", e);
                falha = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            }
        }
        if (urlResposta != null) {
            responderCloudFormation(urlResposta, pedido, dados, falha, contexto);
        } else if (falha != null) {
            throw new IllegalStateException("Falha na migração do banco: " + falha);
        }
        return dados;
    }

    private Map<String, Object> executarComLimite(Duration limite) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Map<String, Object>> futuro = executor.submit(migracao::get);
            return futuro.get(limite.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Tempo esgotado aplicando as migrações.", e);
        } catch (ExecutionException e) {
            throw e.getCause() instanceof RuntimeException r ? r : new IllegalStateException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Migração interrompida.", e);
        } finally {
            executor.shutdownNow();
        }
    }

    private static Duration limite(Context contexto) {
        long restante = contexto == null ? 300_000 : contexto.getRemainingTimeInMillis();
        return Duration.ofMillis(Math.max(1_000, restante - MARGEM_MS));
    }

    /** Protocolo de recursos personalizados: PUT do resultado na URL pré-assinada (sem Content-Type). */
    void responderCloudFormation(String url, Map<String, Object> pedido, Map<String, Object> dados, String falha,
                                 Context contexto) {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("Status", falha == null ? "SUCCESS" : "FAILED");
        String fluxoLog = contexto == null ? "" : " Veja o log " + contexto.getLogStreamName() + ".";
        resposta.put("Reason", falha == null ? "OK" : abreviar(falha, 300) + fluxoLog);
        resposta.put("PhysicalResourceId", Objects.toString(pedido.get("PhysicalResourceId"), ID_FISICO));
        resposta.put("StackId", pedido.get("StackId"));
        resposta.put("RequestId", pedido.get("RequestId"));
        resposta.put("LogicalResourceId", pedido.get("LogicalResourceId"));
        resposta.put("NoEcho", false);
        resposta.put("Data", dados);
        try {
            HttpResponse<String> retorno = http.send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .PUT(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(resposta)))
                    .build(), HttpResponse.BodyHandlers.ofString());
            if (retorno.statusCode() / 100 != 2) {
                LOG.error("CloudFormation recusou a resposta: HTTP {} {}", retorno.statusCode(), retorno.body());
            }
        } catch (java.io.IOException e) {
            LOG.error("Falha ao responder ao CloudFormation.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Interrompido ao responder ao CloudFormation.", e);
        }
    }

    private static String texto(Object valor) {
        return valor == null || valor.toString().isBlank() ? null : valor.toString();
    }

    private static String abreviar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo) + "…";
    }
}
