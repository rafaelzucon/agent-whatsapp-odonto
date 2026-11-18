package br.com.consultorio.odonto.adaptador.entrada.lambda;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;

/** Serializa e desserializa {@link MensagemEnfileirada} em JSON (datas em ISO-8601). */
public class CodecMensagemFila {

    private final JsonMapper json;

    public CodecMensagemFila(JsonMapper json) {
        this.json = Objects.requireNonNull(json);
    }

    public String paraJson(MensagemEnfileirada mensagem) {
        return json.writeValueAsString(mensagem);
    }

    /** @throws IllegalArgumentException se o corpo não for uma mensagem válida */
    public MensagemEnfileirada deJson(String corpo) {
        MensagemEnfileirada mensagem;
        try {
            mensagem = json.readValue(corpo, MensagemEnfileirada.class);
        } catch (JacksonException | NullPointerException e) {
            throw new IllegalArgumentException("Mensagem da fila inválida.", e);
        }
        if (mensagem == null) {
            throw new IllegalArgumentException("Mensagem da fila vazia.");
        }
        return mensagem;
    }
}
