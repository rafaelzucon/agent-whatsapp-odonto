package br.com.consultorio.odonto.adaptador.saida.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Período em que a agenda não aceita consultas (férias, curso, manutenção). */
@Entity
@Table(name = "bloqueio_agenda")
public class BloqueioAgendaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fim;

    @Column(nullable = false)
    private String motivo;

    protected BloqueioAgendaEntity() {
    }

    public BloqueioAgendaEntity(Long id, Instant inicio, Instant fim, String motivo) {
        this.id = id;
        this.inicio = inicio;
        this.fim = fim;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFim() {
        return fim;
    }

    public String getMotivo() {
        return motivo;
    }
}
