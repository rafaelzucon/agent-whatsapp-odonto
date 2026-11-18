package br.com.consultorio.odonto.aplicacao.casodeuso.classificacao;

import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;
import br.com.consultorio.odonto.aplicacao.porta.saida.ClassificacaoIA;
import br.com.consultorio.odonto.aplicacao.porta.saida.ClassificadorProblemaIA;
import br.com.consultorio.odonto.dominio.modelo.ClassificacaoQueixa;
import br.com.consultorio.odonto.dominio.modelo.OrigemClassificacao;
import br.com.consultorio.odonto.dominio.modelo.ProblemaOdontologico;

import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Classificação pela IA. O resultado só é aceito se o código existir no catálogo e a confiança atingir o mínimo;
 * a prioridade é a mais alta entre a do item do catálogo e a indicada pela IA.
 */
public final class ClassificacaoPorIA implements EstrategiaClassificacao {

    private static final System.Logger LOG = System.getLogger(ClassificacaoPorIA.class.getName());

    private final ClassificadorProblemaIA classificadorIA;
    private final double confiancaMinima;

    public ClassificacaoPorIA(ClassificadorProblemaIA classificadorIA, double confiancaMinima) {
        this.classificadorIA = Objects.requireNonNull(classificadorIA);
        this.confiancaMinima = confiancaMinima;
    }

    @Override
    public Optional<ClassificacaoQueixa> classificar(String texto, List<ProblemaOdontologico> catalogo) {
        ClassificacaoIA resultado;
        try {
            resultado = classificadorIA.classificar(texto, catalogo);
        } catch (FalhaIntegracaoException e) {
            LOG.log(Level.WARNING, "Classificação por IA indisponível, usando palavras-chave: {0}", e.getMessage());
            return Optional.empty();
        }
        if (resultado.confianca() < confiancaMinima) {
            return Optional.empty();
        }
        return catalogo.stream()
                .filter(p -> p.codigo().equals(resultado.codigo()))
                .findFirst()
                .map(p -> new ClassificacaoQueixa(p, resultado.confianca(), OrigemClassificacao.IA,
                        p.prioridade().maisAltaEntre(resultado.prioridadeIndicada())));
    }
}
