package br.com.consultorio.odonto.aplicacao.excecao;

/**
 * A mensagem com este {@code wa_message_id} já foi gravada (a Meta reenviou o webhook ou duas entregas
 * chegaram ao mesmo tempo). Usada para garantir a idempotência do recebimento.
 */
public class MensagemDuplicadaException extends RuntimeException {

    public MensagemDuplicadaException(String mensagem) {
        super(mensagem);
    }
}
