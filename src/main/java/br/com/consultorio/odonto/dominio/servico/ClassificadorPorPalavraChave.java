package br.com.consultorio.odonto.dominio.servico;

import br.com.consultorio.odonto.dominio.modelo.ClassificacaoQueixa;
import br.com.consultorio.odonto.dominio.modelo.OrigemClassificacao;
import br.com.consultorio.odonto.dominio.modelo.ProblemaOdontologico;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Classificação determinística da queixa pelas palavras-chave do catálogo. Usada quando a IA falha
 * ou tem baixa confiança. Expressões mais longas (mais palavras) pesam mais; empates favorecem a
 * prioridade mais alta.
 */
public final class ClassificadorPorPalavraChave {

    private static final double CONFIANCA_BASE = 0.5;
    private static final double CONFIANCA_POR_PONTO = 0.1;
    private static final double CONFIANCA_MAXIMA = 0.9;

    public Optional<ClassificacaoQueixa> classificar(String queixa, List<ProblemaOdontologico> catalogo) {
        String texto = NormalizadorTexto.normalizar(queixa);
        if (texto.isEmpty()) {
            return Optional.empty();
        }
        return catalogo.stream()
                .filter(p -> !p.ehAvaliacaoGeral())
                .map(p -> new Pontuacao(p, pontuar(texto, p)))
                .filter(p -> p.pontos() > 0)
                .max(Comparator.comparingInt(Pontuacao::pontos)
                        .thenComparing(p -> -p.problema().prioridade().ordinal()))
                .map(p -> new ClassificacaoQueixa(p.problema(),
                        Math.min(CONFIANCA_MAXIMA, CONFIANCA_BASE + CONFIANCA_POR_PONTO * p.pontos()),
                        OrigemClassificacao.PALAVRA_CHAVE, p.problema().prioridade()));
    }

    private static int pontuar(String texto, ProblemaOdontologico problema) {
        return problema.palavrasChave().stream()
                .filter(p -> NormalizadorTexto.contemExpressao(texto, p))
                .mapToInt(p -> NormalizadorTexto.normalizar(p).split(" ").length)
                .sum();
    }

    private record Pontuacao(ProblemaOdontologico problema, int pontos) {
    }
}
