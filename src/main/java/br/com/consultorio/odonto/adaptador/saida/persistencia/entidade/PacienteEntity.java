package br.com.consultorio.odonto.adaptador.saida.persistencia.entidade;

import br.com.consultorio.odonto.adaptador.saida.persistencia.cripto.TextoCriptografadoConverter;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/** Paciente. RG, nome, data de nascimento e responsável são gravados cifrados. */
@Entity
@Table(name = "paciente")
public class PacienteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rg_hash", nullable = false, unique = true)
    private String rgHash;

    @Convert(converter = TextoCriptografadoConverter.class)
    @Column(name = "rg_cifrado", nullable = false, columnDefinition = "text")
    private String rg;

    @Convert(converter = TextoCriptografadoConverter.class)
    @Column(name = "nome_cifrado", nullable = false, columnDefinition = "text")
    private String nome;

    @Convert(converter = TextoCriptografadoConverter.class)
    @Column(name = "data_nascimento_cifrada", nullable = false, columnDefinition = "text")
    private String dataNascimento;

    @Convert(converter = TextoCriptografadoConverter.class)
    @Column(name = "responsavel_cifrado", columnDefinition = "text")
    private String responsavel;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "paciente_telefone", joinColumns = @JoinColumn(name = "paciente_id"))
    @Column(name = "wa_id", nullable = false)
    private Set<String> telefones = new LinkedHashSet<>();

    @Column(name = "consentimento_lgpd", nullable = false)
    private boolean consentimentoLgpd;

    @Column(name = "data_consentimento")
    private Instant dataConsentimento;

    @Column(name = "data_cadastro", nullable = false)
    private Instant dataCadastro;

    protected PacienteEntity() {
    }

    public PacienteEntity(Long id, String rgHash, String rg, String nome, String dataNascimento, String responsavel,
                          Set<String> telefones, boolean consentimentoLgpd, Instant dataConsentimento,
                          Instant dataCadastro) {
        this.id = id;
        this.rgHash = rgHash;
        this.rg = rg;
        this.nome = nome;
        this.dataNascimento = dataNascimento;
        this.responsavel = responsavel;
        this.telefones = new LinkedHashSet<>(telefones);
        this.consentimentoLgpd = consentimentoLgpd;
        this.dataConsentimento = dataConsentimento;
        this.dataCadastro = dataCadastro;
    }

    public Long getId() {
        return id;
    }

    public String getRgHash() {
        return rgHash;
    }

    public String getRg() {
        return rg;
    }

    public String getNome() {
        return nome;
    }

    public String getDataNascimento() {
        return dataNascimento;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public Set<String> getTelefones() {
        return telefones;
    }

    public boolean isConsentimentoLgpd() {
        return consentimentoLgpd;
    }

    public Instant getDataConsentimento() {
        return dataConsentimento;
    }

    public Instant getDataCadastro() {
        return dataCadastro;
    }
}
