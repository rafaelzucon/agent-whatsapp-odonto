package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.excecao.TransicaoInvalidaException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Consulta agendada na agenda do consultório.
 * Transições permitidas: AGENDADA → CANCELADA, AGENDADA → REALIZADA, AGENDADA → FALTOU.
 */
public final class Consulta {

    private final Long id;
    private final long pacienteId;
    private final Long queixaId;
    private final Horario horario;
    private final Instant criadaEm;
    private StatusConsulta status;

    private Consulta(Long id, long pacienteId, Long queixaId, Horario horario, StatusConsulta status, Instant criadaEm) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.queixaId = queixaId;
        this.horario = Objects.requireNonNull(horario, "horario");
        this.status = Objects.requireNonNull(status, "status");
        this.criadaEm = Objects.requireNonNull(criadaEm, "criadaEm");
    }

    public static Consulta agendar(long pacienteId, Optional<Long> queixaId, Horario horario, Instant agora) {
        return new Consulta(null, pacienteId, queixaId.orElse(null), horario, StatusConsulta.AGENDADA, agora);
    }

    public static Consulta restaurar(Long id, long pacienteId, Long queixaId, Horario horario, StatusConsulta status,
                                     Instant criadaEm) {
        return new Consulta(Objects.requireNonNull(id, "id"), pacienteId, queixaId, horario, status, criadaEm);
    }

    public void cancelar() {
        exigirAgendada("cancelar");
        status = StatusConsulta.CANCELADA;
    }

    public void marcarRealizada(LocalDateTime agora) {
        exigirAgendada("marcar como realizada");
        exigirIniciada(agora);
        status = StatusConsulta.REALIZADA;
    }

    public void marcarFalta(LocalDateTime agora) {
        exigirAgendada("marcar falta");
        exigirIniciada(agora);
        status = StatusConsulta.FALTOU;
    }

    /** Diz se esta consulta ocupa (bloqueia) o horário informado na agenda. */
    public boolean ocupa(Horario outro) {
        return estaAgendada() && horario.sobrepoe(outro);
    }

    public boolean estaAgendada() {
        return status == StatusConsulta.AGENDADA;
    }

    private void exigirAgendada(String acao) {
        if (!estaAgendada()) {
            throw new TransicaoInvalidaException("Não é possível " + acao + " uma consulta com status " + status + ".");
        }
    }

    private void exigirIniciada(LocalDateTime agora) {
        if (agora.isBefore(horario.inicio())) {
            throw new TransicaoInvalidaException("A consulta ainda não começou.");
        }
    }

    public Optional<Long> id() {
        return Optional.ofNullable(id);
    }

    public long pacienteId() {
        return pacienteId;
    }

    public Optional<Long> queixaId() {
        return Optional.ofNullable(queixaId);
    }

    public Horario horario() {
        return horario;
    }

    public StatusConsulta status() {
        return status;
    }

    public Instant criadaEm() {
        return criadaEm;
    }
}
