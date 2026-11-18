package br.com.consultorio.odonto.aplicacao.porta.saida;

import java.time.Instant;

/** Remoção do histórico de mensagens do WhatsApp pela política de retenção da LGPD. */
public interface RetencaoMensagens {

    /** Apaga as mensagens criadas antes de {@code limite}; devolve quantas. */
    int removerAnterioresA(Instant limite);
}
