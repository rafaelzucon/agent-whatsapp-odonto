package br.com.consultorio.odonto.aplicacao.casodeuso.classificacao;

import br.com.consultorio.odonto.dominio.modelo.ClassificacaoQueixa;
import br.com.consultorio.odonto.dominio.modelo.ProblemaOdontologico;
import br.com.consultorio.odonto.dominio.servico.ClassificadorPorPalavraChave;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Classificação determinística pelas palavras-chave do catálogo (serviço de domínio). */
public final class ClassificacaoPorPalavraChave implements EstrategiaClassificacao {

    private final ClassificadorPorPalavraChave classificador;

    public ClassificacaoPorPalavraChave(ClassificadorPorPalavraChave classificador) {
        this.classificador = Objects.requireNonNull(classificador);
    }

    @Override
    public Optional<ClassificacaoQueixa> classificar(String texto, List<ProblemaOdontologico> catalogo) {
        return classificador.classificar(texto, catalogo);
    }
}
