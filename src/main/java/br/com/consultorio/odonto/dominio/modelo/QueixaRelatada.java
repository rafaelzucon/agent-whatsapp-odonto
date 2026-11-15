package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Queixa (motivo da consulta) relatada pelo paciente e registrada no prontuário.
 * Alterada apenas pelo agregado {@link Prontuario}.
 */
public final class QueixaRelatada {

    private final Long id;
    private final String textoOriginal;
    private final Instant dataRegistro;
    private ProblemaOdontologico problema;
    private Prioridade prioridade;
    private double confianca;
    private OrigemClassificacao origem;
    private Long consultaId;

    private QueixaRelatada(Long id, String textoOriginal, ProblemaOdontologico problema, Prioridade prioridade,
                           double confianca, OrigemClassificacao origem, Instant dataRegistro, Long consultaId) {
        if (textoOriginal == null || textoOriginal.isBlank()) {
            throw new DadoInvalidoException("A queixa não pode ser vazia.");
        }
        this.id = id;
        this.textoOriginal = textoOriginal.trim();
        this.problema = Objects.requireNonNull(problema, "problema");
        this.prioridade = Objects.requireNonNull(prioridade, "prioridade");
        this.confianca = confianca;
        this.origem = Objects.requireNonNull(origem, "origem");
        this.dataRegistro = Objects.requireNonNull(dataRegistro, "dataRegistro");
        this.consultaId = consultaId;
    }

    static QueixaRelatada nova(String texto, ClassificacaoQueixa classificacao, Instant agora) {
        return new QueixaRelatada(null, texto, classificacao.problema(), classificacao.prioridade(),
                classificacao.confianca(), classificacao.origem(), agora, null);
    }

    public static QueixaRelatada restaurar(Long id, String texto, ProblemaOdontologico problema, Prioridade prioridade,
                                           double confianca, OrigemClassificacao origem, Instant dataRegistro,
                                           Long consultaId) {
        return new QueixaRelatada(Objects.requireNonNull(id, "id"), texto, problema, prioridade, confianca, origem,
                dataRegistro, consultaId);
    }

    void reclassificar(ProblemaOdontologico novoProblema, Prioridade novaPrioridade) {
        this.problema = Objects.requireNonNull(novoProblema, "novoProblema");
        this.prioridade = novoProblema.prioridade().maisAltaEntre(novaPrioridade);
        this.confianca = 1.0;
        this.origem = OrigemClassificacao.MANUAL;
    }

    void vincularConsulta(long idConsulta) {
        this.consultaId = idConsulta;
    }

    public Optional<Long> id() {
        return Optional.ofNullable(id);
    }

    public String textoOriginal() {
        return textoOriginal;
    }

    public ProblemaOdontologico problema() {
        return problema;
    }

    public Prioridade prioridade() {
        return prioridade;
    }

    public double confianca() {
        return confianca;
    }

    public OrigemClassificacao origem() {
        return origem;
    }

    public Instant dataRegistro() {
        return dataRegistro;
    }

    public Optional<Long> consultaId() {
        return Optional.ofNullable(consultaId);
    }
}
