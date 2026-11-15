package br.com.consultorio.odonto.aplicacao.porta.entrada;

import br.com.consultorio.odonto.dominio.modelo.TipoConteudo;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Mensagem recebida no webhook, já traduzida do formato da Meta.
 * Use as fábricas {@link #texto}, {@link #audio} e {@link #outro}.
 */
public record MensagemRecebidaCommand(String waMessageId, String waId, TipoConteudo tipo, Optional<String> texto,
                                      Optional<String> midiaId, Instant recebidaEm) {

    public MensagemRecebidaCommand {
        Objects.requireNonNull(waMessageId, "waMessageId");
        Objects.requireNonNull(waId, "waId");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(texto, "texto");
        Objects.requireNonNull(midiaId, "midiaId");
        Objects.requireNonNull(recebidaEm, "recebidaEm");
    }

    public static MensagemRecebidaCommand texto(String waMessageId, String waId, String texto, Instant recebidaEm) {
        return new MensagemRecebidaCommand(waMessageId, waId, TipoConteudo.TEXTO, Optional.ofNullable(texto),
                Optional.empty(), recebidaEm);
    }

    public static MensagemRecebidaCommand audio(String waMessageId, String waId, String midiaId, Instant recebidaEm) {
        return new MensagemRecebidaCommand(waMessageId, waId, TipoConteudo.AUDIO, Optional.empty(),
                Optional.ofNullable(midiaId), recebidaEm);
    }

    /** Imagem, figurinha, documento, localização etc.: o agente pede resposta por texto. */
    public static MensagemRecebidaCommand outro(String waMessageId, String waId, Instant recebidaEm) {
        return new MensagemRecebidaCommand(waMessageId, waId, TipoConteudo.OUTRO, Optional.empty(),
                Optional.empty(), recebidaEm);
    }
}
