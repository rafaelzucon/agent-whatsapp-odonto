package br.com.consultorio.odonto.adaptador.entrada.lambda;

import br.com.consultorio.odonto.adaptador.entrada.webhook.AssinaturaWebhookVerifier;
import br.com.consultorio.odonto.adaptador.entrada.webhook.ConfiguracaoWebhook;
import br.com.consultorio.odonto.adaptador.entrada.webhook.ConversorWebhook;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class WebhookFilaAdapterTest {

    private static final String SEGREDO = "segredo-app";
    private static final byte[] CORPO = """
            {"object":"whatsapp_business_account","entry":[{"id":"WABA","changes":[{"field":"messages",
             "value":{"messaging_product":"whatsapp","metadata":{"phone_number_id":"PHONE1"},
             "messages":[{"from":"5511999990000","id":"wamid.1","timestamp":"1763294400","type":"text",
             "text":{"body":"Oi"}},{"from":"5511888880000","id":"wamid.2","timestamp":"1763294401",
             "type":"audio","audio":{"id":"MIDIA9"}}]}}]}]}
            """.getBytes(StandardCharsets.UTF_8);

    private final List<MensagemEnfileirada> enfileiradas = new ArrayList<>();
    private final ConfiguracaoWebhook configuracao = new ConfiguracaoWebhook("token-verif", SEGREDO, "PHONE1");
    private final WebhookFilaAdapter adaptador = new WebhookFilaAdapter(new AssinaturaWebhookVerifier(configuracao),
            new ConversorWebhook(JsonMapper.builder().build(), configuracao), enfileiradas::addAll);

    private static String assinar(byte[] corpo) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SEGREDO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(corpo));
    }

    @Test
    void respondeAoDesafioDeVerificacaoSoComOTokenCerto() {
        Map<String, String> parametros = Map.of("hub.mode", "subscribe", "hub.challenge", "12345",
                "hub.verify_token", "token-verif");

        assertThat(adaptador.tratar(new RequisicaoHttp("get", parametros, null, null)))
                .isEqualTo(new RespostaHttp(200, "12345"));
        assertThat(adaptador.tratar(new RequisicaoHttp("GET", Map.of("hub.mode", "subscribe",
                "hub.challenge", "12345", "hub.verify_token", "errado"), null, null)).status()).isEqualTo(403);
    }

    @Test
    void enfileiraCadaMensagemComAssinaturaValidaECabecalhoEmMinusculas() throws Exception {
        RespostaHttp resposta = adaptador.tratar(new RequisicaoHttp("POST", null,
                Map.of("x-hub-signature-256", assinar(CORPO)), CORPO));

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(enfileiradas).extracting(MensagemEnfileirada::waMessageId, MensagemEnfileirada::waId,
                        MensagemEnfileirada::tipo, MensagemEnfileirada::texto, MensagemEnfileirada::midiaId)
                .containsExactly(
                        tuple("wamid.1", "5511999990000", "TEXTO", "Oi", null),
                        tuple("wamid.2", "5511888880000", "AUDIO", null, "MIDIA9"));
    }

    @Test
    void recusaAssinaturaInvalidaSemEnfileirar() {
        RespostaHttp resposta = adaptador.tratar(new RequisicaoHttp("POST", null,
                Map.of("X-Hub-Signature-256", "sha256=00"), CORPO));

        assertThat(resposta.status()).isEqualTo(401);
        assertThat(enfileiradas).isEmpty();
    }

    @Test
    void descartaCorpoInvalidoComSucessoParaAMetaNaoReenviar() throws Exception {
        byte[] lixo = "não é json".getBytes(StandardCharsets.UTF_8);

        assertThat(adaptador.tratar(new RequisicaoHttp("POST", null, Map.of("X-Hub-Signature-256", assinar(lixo)),
                lixo)).status()).isEqualTo(200);
        assertThat(enfileiradas).isEmpty();
    }

    @Test
    void recusaOutrosMetodos() {
        assertThat(adaptador.tratar(new RequisicaoHttp("PUT", null, null, CORPO)).status()).isEqualTo(405);
    }
}
