package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Prontuário do paciente (um por paciente), com o histórico de queixas relatadas.
 */
public final class Prontuario {

    private final Long id;
    private final long pacienteId;
    private final String numero;
    private final Instant dataAbertura;
    private final List<QueixaRelatada> queixas;

    private Prontuario(Long id, long pacienteId, String numero, Instant dataAbertura, List<QueixaRelatada> queixas) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.numero = Objects.requireNonNull(numero, "numero");
        this.dataAbertura = Objects.requireNonNull(dataAbertura, "dataAbertura");
        this.queixas = new ArrayList<>(queixas);
    }

    /** Abre o prontuário de um paciente recém-cadastrado, com número legível (ex.: PR-000123). */
    public static Prontuario abrir(long pacienteId, long sequencial, Instant agora) {
        return new Prontuario(null, pacienteId, formatarNumero(sequencial), agora, List.of());
    }

    public static Prontuario restaurar(Long id, long pacienteId, String numero, Instant dataAbertura,
                                       List<QueixaRelatada> queixas) {
        return new Prontuario(Objects.requireNonNull(id, "id"), pacienteId, numero, dataAbertura, queixas);
    }

    public static String formatarNumero(long sequencial) {
        if (sequencial <= 0) {
            throw new DadoInvalidoException("O sequencial do prontuário deve ser positivo.");
        }
        return "PR-%06d".formatted(sequencial);
    }

    public QueixaRelatada registrarQueixa(String texto, ClassificacaoQueixa classificacao, Instant agora) {
        QueixaRelatada queixa = QueixaRelatada.nova(texto, classificacao, agora);
        queixas.add(queixa);
        return queixa;
    }

    /** Correção manual da classificação feita pelo dentista. */
    public QueixaRelatada reclassificarQueixa(long queixaId, ProblemaOdontologico problema) {
        QueixaRelatada queixa = exigirQueixa(queixaId);
        queixa.reclassificar(problema, problema.prioridade());
        return queixa;
    }

    public void vincularConsulta(long queixaId, long consultaId) {
        exigirQueixa(queixaId).vincularConsulta(consultaId);
    }

    public Optional<QueixaRelatada> queixa(long queixaId) {
        return queixas.stream().filter(q -> q.id().filter(id -> id == queixaId).isPresent()).findFirst();
    }

    /** A queixa mais recente (a última registrada). */
    public Optional<QueixaRelatada> ultimaQueixa() {
        return queixas.isEmpty() ? Optional.empty() : Optional.of(queixas.getLast());
    }

    private QueixaRelatada exigirQueixa(long queixaId) {
        return queixa(queixaId).orElseThrow(
                () -> new DadoInvalidoException("A queixa " + queixaId + " não pertence ao prontuário " + numero + "."));
    }

    public Optional<Long> id() {
        return Optional.ofNullable(id);
    }

    public long pacienteId() {
        return pacienteId;
    }

    public String numero() {
        return numero;
    }

    public Instant dataAbertura() {
        return dataAbertura;
    }

    public List<QueixaRelatada> queixas() {
        return Collections.unmodifiableList(queixas);
    }
}
