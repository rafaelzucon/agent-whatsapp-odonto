package br.com.consultorio.odonto.adaptador.saida.whatsapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Corpos de requisição e resposta da Cloud API usados pelo adaptador. */
final class CloudApiDtos {

    private CloudApiDtos() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record EnvioMensagem(@JsonProperty("messaging_product") String produto,
                         @JsonProperty("recipient_type") String tipoDestinatario,
                         String to, String type, Texto text, Template template) {

        static EnvioMensagem texto(String destino, String corpo) {
            return new EnvioMensagem("whatsapp", "individual", destino, "text", new Texto(false, corpo), null);
        }

        static EnvioMensagem template(String destino, Template template) {
            return new EnvioMensagem("whatsapp", "individual", destino, "template", null, template);
        }
    }

    record Texto(@JsonProperty("preview_url") boolean previewUrl, String body) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    record Template(String name, Idioma language, List<Componente> components) {
    }

    record Idioma(String code) {
    }

    record Componente(String type, List<Parametro> parameters) {
    }

    record Parametro(String type, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaEnvio(List<MensagemEnviada> messages) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MensagemEnviada(String id) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record InfoMidia(String url, @JsonProperty("mime_type") String mimeType,
                     @JsonProperty("file_size") Long tamanho) {
    }
}
