package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;
import br.com.consultorio.odonto.dominio.modelo.WaId;

import java.util.List;

/** Envia templates aprovados pela Meta (permitidos fora da janela de 24h). */
public interface EnviadorTemplateWhatsApp {

    /**
     * Envia um template aprovado.
     *
     * @param parametrosCorpo valores de {{1}}, {{2}}, ... do corpo, na ordem
     * @return o id da mensagem na Meta (wamid); nunca {@code null}
     * @throws FalhaIntegracaoException em erro de rede ou resposta de erro da API
     */
    String enviarTemplate(WaId destino, String nomeTemplate, String idioma, List<String> parametrosCorpo);
}
