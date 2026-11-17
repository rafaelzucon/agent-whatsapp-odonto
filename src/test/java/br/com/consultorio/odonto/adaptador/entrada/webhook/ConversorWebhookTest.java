package br.com.consultorio.odonto.adaptador.entrada.webhook;

import br.com.consultorio.odonto.aplicacao.porta.entrada.MensagemRecebidaCommand;
import br.com.consultorio.odonto.dominio.modelo.TipoConteudo;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversorWebhookTest {

    private static final Instant AGORA = Instant.parse("2026-11-16T12:00:00Z");

    private final ConversorWebhook conversor = new ConversorWebhook(JsonMapper.builder().build(),
            new ConfiguracaoWebhook("t", "s", "PHONE1"), Clock.fixed(AGORA, ZoneOffset.UTC));

    private static byte[] payload(String phoneNumberId, String mensagens) {
        return """
                {"object":"whatsapp_business_account","entry":[{"id":"WABA","changes":[{"field":"messages",
                 "value":{"messaging_product":"whatsapp","metadata":{"display_phone_number":"5511","phone_number_id":"%s"},
                 "contacts":[{"wa_id":"5511999990000","profile":{"name":"Ana"}}],
                 "messages":[%s]}}]}]}
                """.formatted(phoneNumberId, mensagens).getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void converteTextoComHorarioDaMeta() {
        List<MensagemRecebidaCommand> comandos = conversor.converter(payload("PHONE1", """
                {"from":"5511999990000","id":"wamid.1","timestamp":"1763294400","type":"text","text":{"body":"Oi"}}
                """));

        assertThat(comandos).singleElement().satisfies(c -> {
            assertThat(c.waMessageId()).isEqualTo("wamid.1");
            assertThat(c.waId()).isEqualTo("5511999990000");
            assertThat(c.tipo()).isEqualTo(TipoConteudo.TEXTO);
            assertThat(c.texto()).contains("Oi");
            assertThat(c.recebidaEm()).isEqualTo(Instant.ofEpochSecond(1763294400L));
        });
    }

    @Test
    void converteAudioInterativoBotaoEOutrosTipos() {
        List<MensagemRecebidaCommand> comandos = conversor.converter(payload("PHONE1", """
                {"from":"55119","id":"a","type":"audio","audio":{"id":"MIDIA9","mime_type":"audio/ogg","voice":true}},
                {"from":"55119","id":"b","type":"interactive","interactive":{"type":"button_reply",
                 "button_reply":{"id":"sim","title":"Sim"}}},
                {"from":"55119","id":"c","type":"button","button":{"text":"Confirmar","payload":"x"}},
                {"from":"55119","id":"d","type":"image","image":{"id":"IMG"}}
                """));

        assertThat(comandos).extracting(MensagemRecebidaCommand::tipo)
                .containsExactly(TipoConteudo.AUDIO, TipoConteudo.TEXTO, TipoConteudo.TEXTO, TipoConteudo.OUTRO);
        assertThat(comandos.get(0).midiaId()).contains("MIDIA9");
        assertThat(comandos.get(1).texto()).contains("Sim");
        assertThat(comandos.get(2).texto()).contains("Confirmar");
        assertThat(comandos.get(3).recebidaEm()).isEqualTo(AGORA);
    }

    @Test
    void ignoraStatusDeEntregaEOutroNumeroDoConsultorio() {
        byte[] status = """
                {"object":"whatsapp_business_account","entry":[{"changes":[{"field":"messages",
                 "value":{"metadata":{"phone_number_id":"PHONE1"},"statuses":[{"id":"wamid.x","status":"read"}]}}]}]}
                """.getBytes(StandardCharsets.UTF_8);
        assertThat(conversor.converter(status)).isEmpty();
        assertThat(conversor.converter(payload("OUTRO", """
                {"from":"55119","id":"a","type":"text","text":{"body":"Oi"}}
                """))).isEmpty();
    }

    @Test
    void jsonInvalidoLancaIllegalArgument() {
        assertThatThrownBy(() -> conversor.converter("nao-eh-json".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
