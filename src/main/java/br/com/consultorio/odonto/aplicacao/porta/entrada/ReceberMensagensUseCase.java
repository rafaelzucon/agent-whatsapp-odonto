package br.com.consultorio.odonto.aplicacao.porta.entrada;

import java.util.List;

/** Recebe as mensagens do webhook com idempotência por {@code wa_message_id} e agenda o processamento. */
public interface ReceberMensagensUseCase {

    ResultadoRecebimento receber(List<MensagemRecebidaCommand> mensagens);

    /** Quantas mensagens eram novas, quantas já tinham sido recebidas e quantas foram ignoradas. */
    record ResultadoRecebimento(int novas, int duplicadas, int ignoradas) {
    }
}
