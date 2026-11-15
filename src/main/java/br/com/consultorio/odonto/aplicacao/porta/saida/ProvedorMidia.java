package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;

/** Baixa mídias recebidas pelo WhatsApp. Separado de {@link EnviadorWhatsApp} (segregação de interface). */
public interface ProvedorMidia {

    /**
     * @param midiaId id da mídia informado no webhook
     * @return o arquivo baixado; nunca {@code null}
     * @throws FalhaIntegracaoException se a mídia não puder ser obtida
     */
    MidiaBaixada baixar(String midiaId);
}
