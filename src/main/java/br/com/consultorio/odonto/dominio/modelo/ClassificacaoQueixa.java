package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;

import java.util.Objects;

/**
 * Resultado da classificação de uma queixa no catálogo.
 * A prioridade nunca é menor que a prioridade do item do catálogo.
 */
public record ClassificacaoQueixa(ProblemaOdontologico problema, double confianca, OrigemClassificacao origem,
                                  Prioridade prioridade) {

    public ClassificacaoQueixa {
        Objects.requireNonNull(problema, "problema");
        Objects.requireNonNull(origem, "origem");
        if (confianca < 0 || confianca > 1 || Double.isNaN(confianca)) {
            throw new DadoInvalidoException("A confiança deve estar entre 0 e 1.");
        }
        prioridade = problema.prioridade().maisAltaEntre(prioridade);
    }

    /** Eleva a prioridade, se a informada for mais alta. */
    public ClassificacaoQueixa elevarPrioridade(Prioridade outra) {
        return new ClassificacaoQueixa(problema, confianca, origem, prioridade.maisAltaEntre(outra));
    }
}
