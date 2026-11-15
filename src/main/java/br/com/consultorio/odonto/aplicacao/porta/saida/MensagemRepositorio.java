package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.aplicacao.excecao.MensagemDuplicadaException;
import br.com.consultorio.odonto.dominio.modelo.Mensagem;

import java.time.Instant;
import java.util.Optional;

/** Mensagens de WhatsApp recebidas e enviadas (texto cifrado). Nunca devolve {@code null}. */
public interface MensagemRepositorio {

    /**
     * Grava a mensagem e devolve a versão gravada, com id.
     *
     * @throws MensagemDuplicadaException se já existe mensagem com o mesmo {@code wa_message_id}
     */
    Mensagem salvar(Mensagem mensagem);

    boolean existePorWaMessageId(String waMessageId);

    Optional<Mensagem> buscarPorId(long id);

    /** Apaga as mensagens criadas antes de {@code limite}; devolve quantas. */
    int removerAnterioresA(Instant limite);
}
