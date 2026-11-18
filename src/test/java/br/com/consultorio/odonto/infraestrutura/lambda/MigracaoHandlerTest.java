package br.com.consultorio.odonto.infraestrutura.lambda;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Protocolo de recurso personalizado do CloudFormation: a função SEMPRE responde, com sucesso ou falha. */
class MigracaoHandlerTest {

    private HttpServer cloudFormation;
    private final AtomicReference<String> metodo = new AtomicReference<>();
    private final AtomicReference<String> respostaRecebida = new AtomicReference<>();
    private final AtomicInteger migracoes = new AtomicInteger();

    @BeforeEach
    void iniciar() throws Exception {
        cloudFormation = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        cloudFormation.createContext("/resposta", troca -> {
            metodo.set(troca.getRequestMethod());
            respostaRecebida.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            troca.sendResponseHeaders(200, -1);
            troca.close();
        });
        cloudFormation.start();
    }

    @AfterEach
    void parar() {
        cloudFormation.stop(0);
    }

    private Map<String, Object> pedido(String tipo) {
        Map<String, Object> pedido = new HashMap<>();
        pedido.put("RequestType", tipo);
        pedido.put("ResponseURL", "http://127.0.0.1:" + cloudFormation.getAddress().getPort() + "/resposta");
        pedido.put("StackId", "pilha");
        pedido.put("RequestId", "req-1");
        pedido.put("LogicalResourceId", "MigracaoBanco");
        return pedido;
    }

    private MigracaoHandler handler(boolean falhar) {
        return new MigracaoHandler(() -> {
            migracoes.incrementAndGet();
            if (falhar) {
                throw new IllegalStateException("Connection refused");
            }
            return Map.of("MigracoesAplicadas", 2, "VersaoEsquema", "2");
        }, HttpClient.newHttpClient());
    }

    private JsonNode resposta() {
        return JsonMapper.builder().build().readTree(respostaRecebida.get());
    }

    @Test
    void criacaoMigraEConfirmaSucesso() {
        handler(false).handleRequest(pedido("Create"), null);

        assertThat(metodo).hasValue("PUT");
        assertThat(resposta().get("Status").asString()).isEqualTo("SUCCESS");
        assertThat(resposta().get("PhysicalResourceId").asString()).isEqualTo(MigracaoHandler.ID_FISICO);
        assertThat(resposta().get("RequestId").asString()).isEqualTo("req-1");
        assertThat(resposta().get("Data").get("MigracoesAplicadas").asInt()).isEqualTo(2);
    }

    @Test
    void falhaNaMigracaoAvisaOCloudFormationEmVezDeDeixaloEsperando() {
        handler(true).handleRequest(pedido("Update"), null);

        assertThat(resposta().get("Status").asString()).isEqualTo("FAILED");
        assertThat(resposta().get("Reason").asString()).contains("Connection refused");
    }

    @Test
    void remocaoDaPilhaNaoMexeNoBanco() {
        handler(false).handleRequest(pedido("Delete"), null);

        assertThat(migracoes).hasValue(0);
        assertThat(resposta().get("Status").asString()).isEqualTo("SUCCESS");
    }

    @Test
    void invocacaoManualDevolveOResultadoOuLancaAFalha() {
        assertThat(handler(false).handleRequest(Map.of(), null)).containsEntry("MigracoesAplicadas", 2);
        assertThatThrownBy(() -> handler(true).handleRequest(null, null)).hasMessageContaining("Connection refused");
    }
}
