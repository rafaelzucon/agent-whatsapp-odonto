package br.com.consultorio.odonto.aplicacao.casodeuso.classificacao;

import br.com.consultorio.odonto.aplicacao.porta.saida.CatalogoProblemasRepositorio;
import br.com.consultorio.odonto.dominio.modelo.ClassificacaoQueixa;
import br.com.consultorio.odonto.dominio.modelo.OrigemClassificacao;
import br.com.consultorio.odonto.dominio.modelo.ProblemaOdontologico;
import br.com.consultorio.odonto.dominio.servico.DetectorSinaisAlarme;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Tenta as estratégias na ordem recebida (ex.: IA e depois palavras-chave):
 * <ol>
 *   <li>a primeira classificação específica (diferente de "avaliação geral") vence;</li>
 *   <li>senão, vale a primeira "avaliação geral" sugerida por alguma estratégia;</li>
 *   <li>sem nenhuma, usa-se o item {@code AVALIACAO_GERAL} do catálogo.</li>
 * </ol>
 * A prioridade final é elevada pelo detector determinístico de sinais de alarme — uma emergência não depende
 * só da IA.
 */
public final class ClassificadorQueixaEmCadeia implements ClassificadorQueixa {

    private final CatalogoProblemasRepositorio catalogo;
    private final List<EstrategiaClassificacao> estrategias;
    private final DetectorSinaisAlarme detector;

    public ClassificadorQueixaEmCadeia(CatalogoProblemasRepositorio catalogo, List<EstrategiaClassificacao> estrategias,
                                       DetectorSinaisAlarme detector) {
        this.catalogo = Objects.requireNonNull(catalogo);
        this.estrategias = List.copyOf(estrategias);
        this.detector = Objects.requireNonNull(detector);
    }

    @Override
    public ClassificacaoQueixa classificar(String texto) {
        List<ProblemaOdontologico> itens = catalogo.listarAtivos();
        Optional<ClassificacaoQueixa> generica = Optional.empty();
        for (EstrategiaClassificacao estrategia : estrategias) {
            Optional<ClassificacaoQueixa> resultado = estrategia.classificar(texto, itens);
            if (resultado.isEmpty()) {
                continue;
            }
            if (!resultado.get().problema().ehAvaliacaoGeral()) {
                return comAlarme(resultado.get(), texto);
            }
            if (generica.isEmpty()) {
                generica = resultado;
            }
        }
        return comAlarme(generica.orElseGet(() -> avaliacaoGeral(itens)), texto);
    }

    private ClassificacaoQueixa comAlarme(ClassificacaoQueixa classificacao, String texto) {
        return classificacao.elevarPrioridade(detector.prioridadePara(texto));
    }

    private static ClassificacaoQueixa avaliacaoGeral(List<ProblemaOdontologico> itens) {
        ProblemaOdontologico geral = itens.stream()
                .filter(ProblemaOdontologico::ehAvaliacaoGeral)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "O catálogo precisa do item " + ProblemaOdontologico.CODIGO_AVALIACAO_GERAL + "."));
        return new ClassificacaoQueixa(geral, 0.0, OrigemClassificacao.PALAVRA_CHAVE, geral.prioridade());
    }
}
