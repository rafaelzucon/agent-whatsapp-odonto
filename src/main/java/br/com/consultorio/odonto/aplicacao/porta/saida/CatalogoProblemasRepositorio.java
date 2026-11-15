package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.dominio.modelo.ProblemaOdontologico;

import java.util.List;
import java.util.Optional;

/** Catálogo de problemas odontológicos (carregado por migração). Nunca devolve {@code null}. */
public interface CatalogoProblemasRepositorio {

    /** Itens ativos, em ordem de código. */
    List<ProblemaOdontologico> listarAtivos();

    Optional<ProblemaOdontologico> buscarPorCodigo(String codigo);
}
