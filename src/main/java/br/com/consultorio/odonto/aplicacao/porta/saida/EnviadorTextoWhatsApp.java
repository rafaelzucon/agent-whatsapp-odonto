package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;
import br.com.consultorio.odonto.dominio.modelo.WaId;

/** Envia texto livre pelo número do consultório na WhatsApp Cloud API. */
public interface EnviadorTextoWhatsApp {

    /**
     * Envia texto livre. Pré-condição: a janela de atendimento de 24h com o destino está aberta.
     *
     * @return o id da mensagem na Meta (wamid); nunca {@code null}
     * @throws FalhaIntegracaoException em erro de rede ou resposta de erro da API
     */
    String enviarTexto(WaId destino, String texto);
}
