package br.com.consultorio.odonto.adaptador.saida.persistencia.entidade;

import br.com.consultorio.odonto.dominio.modelo.StatusConsulta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Consulta da agenda única do consultório. */
@Entity
@Table(name = "consulta")
public class ConsultaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "queixa_id")
    private Long queixaId;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusConsulta status;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    protected ConsultaEntity() {
    }

    public ConsultaEntity(Long id, Long pacienteId, Long queixaId, Instant inicio, Instant fim, StatusConsulta status,
                          Instant criadaEm) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.queixaId = queixaId;
        this.inicio = inicio;
        this.fim = fim;
        this.status = status;
        this.criadaEm = criadaEm;
    }

    public Long getId() {
        return id;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public Long getQueixaId() {
        return queixaId;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFim() {
        return fim;
    }

    public StatusConsulta getStatus() {
        return status;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }
}
