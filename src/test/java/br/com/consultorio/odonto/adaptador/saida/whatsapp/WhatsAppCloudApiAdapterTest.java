package br.com.consultorio.odonto.adaptador.saida.whatsapp;

import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;
import br.com.consultorio.odonto.aplicacao.porta.saida.MidiaBaixada;
import br.com.consultorio.odonto.dominio.modelo.WaId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WhatsAppCloudApiAdapterTest {

    private static final String BASE = "https://graph.exemplo.test";
    private static final WaId DESTINO = new WaId("5511999990001");

    private final RestClient.Builder construtor = RestClient.builder();
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(construtor).build();

    private WhatsAppCloudApiAdapter adaptador(long limiteMidia) {
        return new WhatsAppCloudApiAdapter(construtor,
                new ConfiguracaoWhatsApp(BASE, "v24.0", "123456", "token-secreto", limiteMidia));
    }

    @Test
    void enviaTextoComTokenEDevolveWamid() {
        servidor.expect(requestTo(BASE + "/v24.0/123456/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-secreto"))
                .andExpect(content().json("""
                        {"messaging_product":"whatsapp","recipient_type":"individual","to":"5511999990001",
                         "type":"text","text":{"preview_url":false,"body":"Olá!"}}""", true))
                .andRespond(withSuccess("""
                        {"messaging_product":"whatsapp","contacts":[{"input":"5511999990001","wa_id":"5511999990001"}],
                         "messages":[{"id":"wamid.ABC"}]}""", MediaType.APPLICATION_JSON));

        assertThat(adaptador(1000).enviarTexto(DESTINO, "Olá!")).isEqualTo("wamid.ABC");
        servidor.verify();
    }

    @Test
    void enviaTemplateComParametrosSaneados() {
        servidor.expect(requestTo(BASE + "/v24.0/123456/messages"))
                .andExpect(content().json("""
                        {"type":"template","template":{"name":"novo_agendamento","language":{"code":"pt_BR"},
                         "components":[{"type":"body","parameters":[
                           {"type":"text","text":"Maria Silva"},{"type":"text","text":"20/11/2026 às 10:00"},
                           {"type":"text","text":"-"}]}]}}"""))
                .andRespond(withSuccess("{\"messages\":[{\"id\":\"wamid.T\"}]}", MediaType.APPLICATION_JSON));

        String id = adaptador(1000).enviarTemplate(DESTINO, "novo_agendamento", "pt_BR",
                List.of("Maria\nSilva", "20/11/2026 às 10:00", " "));

        assertThat(id).isEqualTo("wamid.T");
        servidor.verify();
    }

    @Test
    void erroDaApiViraFalhaDeIntegracao() {
        servidor.expect(requestTo(BASE + "/v24.0/123456/messages"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"Re-engagement message\",\"code\":131047}}"));

        assertThatThrownBy(() -> adaptador(1000).enviarTexto(DESTINO, "Olá"))
                .isInstanceOf(FalhaIntegracaoException.class)
                .hasMessageContaining("400").hasMessageContaining("131047")
                .hasMessageNotContaining("token-secreto");
    }

    @Test
    void baixaMidiaEmDuasEtapas() {
        servidor.expect(requestTo(BASE + "/v24.0/MIDIA1"))
                .andExpect(header("Authorization", "Bearer token-secreto"))
                .andRespond(withSuccess("""
                        {"url":"https://lookaside.exemplo.test/arquivo?id=1","mime_type":"audio/ogg; codecs=opus",
                         "file_size":3,"id":"MIDIA1"}""", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo("https://lookaside.exemplo.test/arquivo?id=1"))
                .andExpect(header("Authorization", "Bearer token-secreto"))
                .andRespond(withSuccess(new byte[]{1, 2, 3}, MediaType.APPLICATION_OCTET_STREAM));

        MidiaBaixada midia = adaptador(1000).baixar("MIDIA1");

        assertThat(midia.conteudo()).containsExactly(1, 2, 3);
        assertThat(midia.extensao()).isEqualTo("ogg");
        servidor.verify();
    }

    @Test
    void recusaMidiaAcimaDoLimite() {
        servidor.expect(requestTo(BASE + "/v24.0/MIDIA2"))
                .andRespond(withSuccess("{\"url\":\"https://lookaside.exemplo.test/x\",\"mime_type\":\"audio/ogg\","
                        + "\"file_size\":5000}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adaptador(1000).baixar("MIDIA2"))
                .isInstanceOf(FalhaIntegracaoException.class).hasMessageContaining("limite");
    }

    @Test
    void semConfiguracaoNaoChamaAApi() {
        WhatsAppCloudApiAdapter semToken = new WhatsAppCloudApiAdapter(construtor,
                new ConfiguracaoWhatsApp(BASE, "v24.0", "", "", 1000));

        assertThatThrownBy(() -> semToken.enviarTexto(DESTINO, "Olá"))
                .isInstanceOf(FalhaIntegracaoException.class).hasMessageContaining("WHATSAPP_ACCESS_TOKEN");
        assertThat(new ConfiguracaoWhatsApp(BASE, "v24.0", "1", "token-secreto", 1).toString())
                .doesNotContain("token-secreto");
        servidor.verify();
    }
}
