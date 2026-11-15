package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.porta.entrada.ConsultarCatalogoUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ProblemaVisao;
import br.com.consultorio.odonto.aplicacao.porta.saida.CatalogoProblemasRepositorio;

import java.util.List;
import java.util.Objects;

/** Lista o catálogo de problemas odontológicos ativos. */
public final class ConsultarCatalogoService implements ConsultarCatalogoUseCase {

    private final CatalogoProblemasRepositorio catalogo;

    public ConsultarCatalogoService(CatalogoProblemasRepositorio catalogo) {
        this.catalogo = Objects.requireNonNull(catalogo);
    }

    @Override
    public List<ProblemaVisao> listar() {
        return catalogo.listarAtivos().stream().map(MontadorVisoes::problema).toList();
    }
}
