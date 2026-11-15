package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;
import br.com.consultorio.odonto.dominio.modelo.WaId;

import java.util.List;

/** Envia mensagens pelo número do consultório na WhatsApp Cloud API. */
public interface EnviadorWhatsApp {

    /**
     * Envia texto livre. Pré-condição: a janela de atendimento de 24h com o destino está aberta.
     *
     * @return o id da mensagem na Meta (wamid); nunca {@code null}
     * @throws FalhaIntegracaoException em erro de rede ou resposta de erro da API
     */
    String enviarTexto(WaId destino, String texto);

    /**
     * Envia um template aprovado (permitido fora da janela de 24h).
     *
     * @param parametrosCorpo valores de {{1}}, {{2}}, ... do corpo, na ordem
     * @return o id da mensagem na Meta (wamid); nunca {@code null}
     * @throws FalhaIntegracaoException em erro de rede ou resposta de erro da API
     */
    String enviarTemplate(WaId destino, String nomeTemplate, String idioma, List<String> parametrosCorpo);
}
