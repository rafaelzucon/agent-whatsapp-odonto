package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Item do catálogo de problemas odontológicos conhecidos.
 *
 * @param cid10 código CID-10 de referência; vazio quando não houver
 */
public record ProblemaOdontologico(
        String codigo,
        String nome,
        String descricao,
        String especialidade,
        Prioridade prioridade,
        Optional<String> cid10,
        List<String> palavrasChave) {

    /** Item usado quando a queixa não se encaixa em nenhum outro. */
    public static final String CODIGO_AVALIACAO_GERAL = "AVALIACAO_GERAL";

    public ProblemaOdontologico {
        if (codigo == null || codigo.isBlank() || nome == null || nome.isBlank()) {
            throw new DadoInvalidoException("Problema do catálogo precisa de código e nome.");
        }
        Objects.requireNonNull(prioridade, "prioridade");
        descricao = descricao == null ? "" : descricao;
        especialidade = especialidade == null ? "" : especialidade;
        cid10 = cid10 == null ? Optional.empty() : cid10.filter(c -> !c.isBlank());
        palavrasChave = palavrasChave == null ? List.of() : List.copyOf(palavrasChave);
    }

    public boolean ehAvaliacaoGeral() {
        return CODIGO_AVALIACAO_GERAL.equals(codigo);
    }
}
