package br.com.consultorio.odonto.adaptador.saida.persistencia.entidade;

import br.com.consultorio.odonto.adaptador.saida.persistencia.cripto.TextoCriptografadoConverter;
import br.com.consultorio.odonto.dominio.modelo.OrigemClassificacao;
import br.com.consultorio.odonto.dominio.modelo.Prioridade;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** Queixa relatada pelo paciente; o texto original é gravado cifrado. */
@Entity
@Table(name = "queixa_relatada")
public class QueixaRelatadaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prontuario_id", nullable = false)
    private ProntuarioEntity prontuario;

    @Convert(converter = TextoCriptografadoConverter.class)
    @Column(name = "texto_cifrado", nullable = false, columnDefinition = "text")
    private String texto;

    @Column(name = "problema_codigo", nullable = false)
    private String problemaCodigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Prioridade prioridade;

    @Column(nullable = false)
    private double confianca;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemClassificacao origem;

    @Column(name = "data_registro", nullable = false)
    private Instant dataRegistro;

    @Column(name = "consulta_id")
    private Long consultaId;

    protected QueixaRelatadaEntity() {
    }

    public QueixaRelatadaEntity(String texto, Instant dataRegistro) {
        this.texto = texto;
        this.dataRegistro = dataRegistro;
    }

    void vincular(ProntuarioEntity prontuario) {
        this.prontuario = prontuario;
    }

    /** Atualiza o que pode mudar depois do registro: classificação e consulta vinculada. */
    public void atualizar(String problemaCodigo, Prioridade prioridade, double confianca, OrigemClassificacao origem,
                          Long consultaId) {
        this.problemaCodigo = problemaCodigo;
        this.prioridade = prioridade;
        this.confianca = confianca;
        this.origem = origem;
        this.consultaId = consultaId;
    }

    public Long getId() {
        return id;
    }

    public String getTexto() {
        return texto;
    }

    public String getProblemaCodigo() {
        return problemaCodigo;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public double getConfianca() {
        return confianca;
    }

    public OrigemClassificacao getOrigem() {
        return origem;
    }

    public Instant getDataRegistro() {
        return dataRegistro;
    }

    public Long getConsultaId() {
        return consultaId;
    }
}
