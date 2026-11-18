package br.com.consultorio.odonto.adaptador.entrada.lambda;

import java.util.List;

/**
 * Buffer de transporte do adaptador de entrada: a função do webhook só valida e enfileira, e a função da
 * conversa consome. A fila precisa preservar a ordem por número de WhatsApp (ex.: SQS FIFO com grupo = waId)
 * e descartar duplicatas pelo id da mensagem da Meta.
 */
public interface FilaMensagensRecebidas {

    void enfileirar(List<MensagemEnfileirada> mensagens);
}
