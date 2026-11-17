package br.com.consultorio.odonto.adaptador.entrada.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Estrutura (parcial) da notificação de webhook da WhatsApp Cloud API. Campos não usados são ignorados. */
@JsonIgnoreProperties(ignoreUnknown = true)
record WebhookPayload(String object, List<Entrada> entry) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Entrada(String id, List<Alteracao> changes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Alteracao(String field, Valor value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Valor(@JsonProperty("messaging_product") String produto, Metadados metadata, List<Mensagem> messages) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Metadados(@JsonProperty("display_phone_number") String numeroExibido,
                     @JsonProperty("phone_number_id") String phoneNumberId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Mensagem(String from, String id, String timestamp, String type, Texto text, Midia audio,
                    Interativo interactive, Botao button) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Texto(String body) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Midia(String id, @JsonProperty("mime_type") String mimeType, Boolean voice) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Interativo(String type, @JsonProperty("button_reply") Resposta respostaBotao,
                      @JsonProperty("list_reply") Resposta respostaLista) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Resposta(String id, String title) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Botao(String text, String payload) {
    }
}
