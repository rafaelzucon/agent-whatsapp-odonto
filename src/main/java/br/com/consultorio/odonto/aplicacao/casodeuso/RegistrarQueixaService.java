package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoConversa;
import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;
import br.com.consultorio.odonto.aplicacao.excecao.RecursoNaoEncontradoException;
import br.com.consultorio.odonto.aplicacao.porta.entrada.RegistrarQueixaUseCase;
import br.com.consultorio.odonto.aplicacao.porta.saida.CatalogoProblemasRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ClassificacaoIA;
import br.com.consultorio.odonto.aplicacao.porta.saida.ClassificadorProblemaIA;
import br.com.consultorio.odonto.aplicacao.porta.saida.ExecutorTransacional;
import br.com.consultorio.odonto.aplicacao.porta.saida.ProntuarioRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import br.com.consultorio.odonto.dominio.modelo.ClassificacaoQueixa;
import br.com.consultorio.odonto.dominio.modelo.OrigemClassificacao;
import br.com.consultorio.odonto.dominio.modelo.ProblemaOdontologico;
import br.com.consultorio.odonto.dominio.modelo.Prontuario;
import br.com.consultorio.odonto.dominio.modelo.QueixaRelatada;
import br.com.consultorio.odonto.dominio.servico.ClassificadorPorPalavraChave;
import br.com.consultorio.odonto.dominio.servico.DetectorSinaisAlarme;

import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Classifica a queixa no catálogo e a registra no prontuário.
 * <ol>
 *   <li>A IA classifica; o resultado só é aceito se o código existir no catálogo e a confiança atingir o mínimo.</li>
 *   <li>Se a IA falhar, tiver pouca confiança ou indicar só "avaliação geral", tenta-se as palavras-chave.</li>
 *   <li>Sem correspondência, usa-se o item {@code AVALIACAO_GERAL}.</li>
 * </ol>
 * A prioridade final é a mais alta entre a do item do catálogo, a indicada pela IA e a do detector
 * determinístico de sinais de alarme — o detector garante que uma emergência não depende só da IA.
 */
public final class RegistrarQueixaService implements RegistrarQueixaUseCase {

    private static final System.Logger LOG = System.getLogger(RegistrarQueixaService.class.getName());

    private final ProntuarioRepositorio prontuarios;
    private final CatalogoProblemasRepositorio catalogo;
    private final ClassificadorProblemaIA classificadorIA;
    private final ClassificadorPorPalavraChave classificadorPalavraChave;
    private final DetectorSinaisAlarme detector;
    private final ConfiguracaoConversa configuracao;
    private final ExecutorTransacional transacao;
    private final Relogio relogio;

    public RegistrarQueixaService(ProntuarioRepositorio prontuarios, CatalogoProblemasRepositorio catalogo,
                                  ClassificadorProblemaIA classificadorIA,
                                  ClassificadorPorPalavraChave classificadorPalavraChave, DetectorSinaisAlarme detector,
                                  ConfiguracaoConversa configuracao, ExecutorTransacional transacao, Relogio relogio) {
        this.prontuarios = Objects.requireNonNull(prontuarios);
        this.catalogo = Objects.requireNonNull(catalogo);
        this.classificadorIA = Objects.requireNonNull(classificadorIA);
        this.classificadorPalavraChave = Objects.requireNonNull(classificadorPalavraChave);
        this.detector = Objects.requireNonNull(detector);
        this.configuracao = Objects.requireNonNull(configuracao);
        this.transacao = Objects.requireNonNull(transacao);
        this.relogio = Objects.requireNonNull(relogio);
    }

    @Override
    public QueixaRegistrada registrar(long pacienteId, String texto) {
        // A classificação (chamada à IA) fica fora da transação para não segurar conexão com o banco.
        ClassificacaoQueixa classificacao = classificar(texto).elevarPrioridade(detector.prioridadePara(texto));
        QueixaRelatada queixa = transacao.executar(() -> {
            Prontuario prontuario = prontuarios.buscarPorPacienteId(pacienteId).orElseThrow(
                    () -> new RecursoNaoEncontradoException("Prontuário do paciente " + pacienteId + " não encontrado."));
            prontuario.registrarQueixa(texto, classificacao, relogio.agora());
            return prontuarios.salvar(prontuario).ultimaQueixa().orElseThrow();
        });
        return new QueixaRegistrada(queixa.id().orElseThrow(), queixa.problema().codigo(), queixa.problema().nome(),
                queixa.prioridade(), queixa.origem(), queixa.confianca());
    }

    ClassificacaoQueixa classificar(String texto) {
        List<ProblemaOdontologico> itens = catalogo.listarAtivos();
        Optional<ClassificacaoQueixa> daIA = classificarComIA(texto, itens);
        if (daIA.isPresent() && !daIA.get().problema().ehAvaliacaoGeral()) {
            return daIA.get();
        }
        return classificadorPalavraChave.classificar(texto, itens)
                .or(() -> daIA)
                .orElseGet(() -> avaliacaoGeral(itens));
    }

    private Optional<ClassificacaoQueixa> classificarComIA(String texto, List<ProblemaOdontologico> itens) {
        ClassificacaoIA resultado;
        try {
            resultado = classificadorIA.classificar(texto, itens);
        } catch (FalhaIntegracaoException e) {
            LOG.log(Level.WARNING, "Classificação por IA indisponível, usando palavras-chave: {0}", e.getMessage());
            return Optional.empty();
        }
        if (resultado.confianca() < configuracao.confiancaMinima()) {
            return Optional.empty();
        }
        return itens.stream()
                .filter(p -> p.codigo().equals(resultado.codigo()))
                .findFirst()
                .map(p -> new ClassificacaoQueixa(p, resultado.confianca(), OrigemClassificacao.IA,
                        p.prioridade().maisAltaEntre(resultado.prioridadeIndicada())));
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
