package br.com.consultorio.odonto.adaptador.saida.persistencia.entidade;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Prontuário do paciente com as queixas relatadas, em ordem de registro. */
@Entity
@Table(name = "prontuario")
public class ProntuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false, unique = true)
    private Long pacienteId;

    @Column(nullable = false, unique = true)
    private String numero;

    @Column(name = "data_abertura", nullable = false)
    private Instant dataAbertura;

    @OneToMany(mappedBy = "prontuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id")
    private List<QueixaRelatadaEntity> queixas = new ArrayList<>();

    protected ProntuarioEntity() {
    }

    public ProntuarioEntity(Long pacienteId, String numero, Instant dataAbertura) {
        this.pacienteId = pacienteId;
        this.numero = numero;
        this.dataAbertura = dataAbertura;
    }

    public void adicionar(QueixaRelatadaEntity queixa) {
        queixa.vincular(this);
        queixas.add(queixa);
    }

    public Long getId() {
        return id;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public String getNumero() {
        return numero;
    }

    public Instant getDataAbertura() {
        return dataAbertura;
    }

    public List<QueixaRelatadaEntity> getQueixas() {
        return queixas;
    }
}
