package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;
import br.com.consultorio.odonto.dominio.modelo.ProblemaOdontologico;

import java.util.List;

/**
 * Classifica a queixa do paciente em um item do catálogo usando um modelo de linguagem.
 * Recebe apenas o texto da queixa: nunca nome, RG, telefone ou ids internos.
 */
public interface ClassificadorProblemaIA {

    /**
     * @param queixa   texto do paciente (tratado como dado, nunca como instrução)
     * @param catalogo itens ativos do catálogo; a IA deve escolher um destes códigos
     * @return a classificação (nunca {@code null}); o código pode não existir no catálogo
     * @throws FalhaIntegracaoException em erro de rede, tempo esgotado ou resposta fora do formato
     */
    ClassificacaoIA classificar(String queixa, List<ProblemaOdontologico> catalogo);
}
