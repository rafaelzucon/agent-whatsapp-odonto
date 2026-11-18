package br.com.consultorio.odonto.infraestrutura.lambda;

import br.com.consultorio.odonto.adaptador.entrada.lambda.CodecMensagemFila;
import br.com.consultorio.odonto.adaptador.entrada.lambda.RequisicaoHttp;
import br.com.consultorio.odonto.adaptador.entrada.lambda.RespostaHttp;
import br.com.consultorio.odonto.adaptador.entrada.lambda.WebhookFilaAdapter;
import br.com.consultorio.odonto.adaptador.entrada.webhook.AssinaturaWebhookVerifier;
import br.com.consultorio.odonto.adaptador.entrada.webhook.ConfiguracaoWebhook;
import br.com.consultorio.odonto.adaptador.entrada.webhook.ConversorWebhook;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Função do webhook do WhatsApp (Function URL pública). Não sobe o Spring nem acessa o banco: valida a
 * assinatura, converte e enfileira no SQS FIFO. Responde em milissegundos e fica fora da VPC.
 */
public class WebhookHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookHandler.class);

    private final WebhookFilaAdapter adaptador;

    public WebhookHandler() {
        this(montar(SegredosAws.resolvedor(SegredosAws.ler())));
    }

    WebhookHandler(WebhookFilaAdapter adaptador) {
        this.adaptador = Objects.requireNonNull(adaptador);
    }

    static WebhookFilaAdapter montar(UnaryOperator<String> configuracao) {
        ConfiguracaoWebhook webhook = new ConfiguracaoWebhook(configuracao.apply("WHATSAPP_TOKEN_VERIFICACAO"),
                configuracao.apply("WHATSAPP_SEGREDO_APP"), configuracao.apply("WHATSAPP_PHONE_NUMBER_ID"));
        JsonMapper json = JsonMapper.builder().build();
        SqsClient sqs = SqsClient.builder().httpClientBuilder(UrlConnectionHttpClient.builder()).build();
        SqsFilaMensagensRecebidas fila = new SqsFilaMensagensRecebidas(sqs, configuracao.apply("FILA_MENSAGENS_URL"),
                new CodecMensagemFila(json));
        return new WebhookFilaAdapter(new AssinaturaWebhookVerifier(webhook), new ConversorWebhook(json, webhook), fila);
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent evento, Context contexto) {
        RespostaHttp resposta;
        try {
            resposta = adaptador.tratar(paraRequisicao(evento));
        } catch (RuntimeException e) {
            // 500 faz a Meta reenviar o webhook depois (ex.: SQS indisponível).
            LOG.error("Falha ao tratar o webhook.", e);
            resposta = new RespostaHttp(500, "");
        }
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(resposta.status())
                .withHeaders(Map.of("Content-Type", "text/plain; charset=utf-8"))
                .withBody(resposta.corpo())
                .withIsBase64Encoded(false)
                .build();
    }

    static RequisicaoHttp paraRequisicao(APIGatewayV2HTTPEvent evento) {
        String metodo = evento.getRequestContext() != null && evento.getRequestContext().getHttp() != null
                ? evento.getRequestContext().getHttp().getMethod() : null;
        return new RequisicaoHttp(metodo, evento.getQueryStringParameters(), evento.getHeaders(), corpo(evento));
    }

    /** A assinatura da Meta é calculada sobre os bytes exatos do corpo, por isso o Base64 é desfeito aqui. */
    private static byte[] corpo(APIGatewayV2HTTPEvent evento) {
        if (evento.getBody() == null) {
            return new byte[0];
        }
        return evento.getIsBase64Encoded() ? Base64.getDecoder().decode(evento.getBody())
                : evento.getBody().getBytes(StandardCharsets.UTF_8);
    }
}
