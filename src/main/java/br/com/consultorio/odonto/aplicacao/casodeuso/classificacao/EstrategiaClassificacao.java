package br.com.consultorio.odonto.aplicacao.casodeuso.classificacao;

import br.com.consultorio.odonto.dominio.modelo.ClassificacaoQueixa;
import br.com.consultorio.odonto.dominio.modelo.ProblemaOdontologico;

import java.util.List;
import java.util.Optional;

/**
 * Uma forma de classificar a queixa (IA, palavras-chave...). Novas estratégias entram sem alterar o
 * {@link ClassificadorQueixaEmCadeia} nem o caso de uso (OCP).
 */
@FunctionalInterface
public interface EstrategiaClassificacao {

    /** Devolve a classificação ou vazio se esta estratégia não conseguiu classificar com segurança. */
    Optional<ClassificacaoQueixa> classificar(String texto, List<ProblemaOdontologico> catalogo);
}
