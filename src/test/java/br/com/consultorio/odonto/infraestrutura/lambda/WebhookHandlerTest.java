package br.com.consultorio.odonto.infraestrutura.lambda;

import br.com.consultorio.odonto.adaptador.entrada.lambda.FilaMensagensRecebidas;
import br.com.consultorio.odonto.adaptador.entrada.lambda.MensagemEnfileirada;
import br.com.consultorio.odonto.adaptador.entrada.lambda.RequisicaoHttp;
import br.com.consultorio.odonto.adaptador.entrada.lambda.WebhookFilaAdapter;
import br.com.consultorio.odonto.adaptador.entrada.webhook.AssinaturaWebhookVerifier;
import br.com.consultorio.odonto.adaptador.entrada.webhook.ConfiguracaoWebhook;
import br.com.consultorio.odonto.adaptador.entrada.webhook.ConversorWebhook;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent.RequestContext;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookHandlerTest {

    private static APIGatewayV2HTTPEvent evento(String metodo, String corpo, boolean base64) {
        RequestContext.Http http = new RequestContext.Http();
        http.setMethod(metodo);
        RequestContext contexto = new RequestContext();
        contexto.setHttp(http);
        APIGatewayV2HTTPEvent evento = new APIGatewayV2HTTPEvent();
        evento.setRequestContext(contexto);
        evento.setHeaders(Map.of("x-hub-signature-256", "sha256=ab"));
        evento.setQueryStringParameters(Map.of("hub.mode", "subscribe"));
        evento.setBody(corpo);
        evento.setIsBase64Encoded(base64);
        return evento;
    }

    @Test
    void desfazOBase64DaFunctionUrlParaValidarABytesExatos() {
        String original = "{\"texto\":\"ação\"}";
        RequisicaoHttp requisicao = WebhookHandler.paraRequisicao(evento("POST",
                Base64.getEncoder().encodeToString(original.getBytes(StandardCharsets.UTF_8)), true));

        assertThat(new String(requisicao.corpo(), StandardCharsets.UTF_8)).isEqualTo(original);
        assertThat(requisicao.metodo()).isEqualTo("POST");
        assertThat(requisicao.cabecalho("X-Hub-Signature-256")).contains("sha256=ab");
        assertThat(requisicao.parametro("hub.mode")).contains("subscribe");
        assertThat(WebhookHandler.paraRequisicao(evento("POST", original, false)).corpo())
                .isEqualTo(original.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void falhaAoEnfileirarViraHttp500ParaAMetaReenviar() {
        ConfiguracaoWebhook configuracao = new ConfiguracaoWebhook("t", "s", "");
        AssinaturaWebhookVerifier verificador = new AssinaturaWebhookVerifier(configuracao) {
            @Override
            public boolean assinaturaValida(byte[] corpo, String cabecalho) {
                return true;
            }
        };
        FilaMensagensRecebidas filaFora = (List<MensagemEnfileirada> mensagens) -> {
            throw new IllegalStateException("SQS indisponível");
        };
        WebhookHandler handler = new WebhookHandler(new WebhookFilaAdapter(verificador,
                new ConversorWebhook(JsonMapper.builder().build(), configuracao), filaFora));
        String corpo = """
                {"entry":[{"changes":[{"field":"messages","value":{"messages":[{"from":"5511999990000",
                "id":"wamid.1","timestamp":"1763294400","type":"text","text":{"body":"Oi"}}]}}]}]}
                """;

        APIGatewayV2HTTPResponse resposta = handler.handleRequest(evento("POST", corpo, false), null);

        assertThat(resposta.getStatusCode()).isEqualTo(500);
        assertThat(handler.handleRequest(evento("DELETE", null, false), null).getStatusCode()).isEqualTo(405);
    }
}
