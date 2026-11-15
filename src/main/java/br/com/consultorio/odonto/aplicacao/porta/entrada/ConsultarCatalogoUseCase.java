package br.com.consultorio.odonto.aplicacao.porta.entrada;

import java.util.List;

/** Lista o catálogo de problemas odontológicos. */
public interface ConsultarCatalogoUseCase {

    List<ProblemaVisao> listar();
}
