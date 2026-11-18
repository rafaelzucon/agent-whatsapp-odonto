package br.com.consultorio.odonto.aplicacao.casodeuso.classificacao;

import br.com.consultorio.odonto.dominio.modelo.ClassificacaoQueixa;

/** Classifica a queixa do paciente em um item do catálogo de problemas, sempre devolvendo um resultado. */
@FunctionalInterface
public interface ClassificadorQueixa {

    ClassificacaoQueixa classificar(String texto);
}
