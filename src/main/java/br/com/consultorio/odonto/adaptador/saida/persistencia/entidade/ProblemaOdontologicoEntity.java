package br.com.consultorio.odonto.adaptador.saida.persistencia.entidade;

import br.com.consultorio.odonto.dominio.modelo.Prioridade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Item do catálogo de problemas odontológicos (carregado pela migração V2). */
@Entity
@Table(name = "problema_odontologico")
public class ProblemaOdontologicoEntity {

    @Id
    private String codigo;
    private String nome;
    @Column(columnDefinition = "text")
    private String descricao;
    private String especialidade;
    @Enumerated(EnumType.STRING)
    private Prioridade prioridade;
    private String cid10;
    @Column(name = "palavras_chave", columnDefinition = "text")
    private String palavrasChave;
    private boolean ativo;

    protected ProblemaOdontologicoEntity() {
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getEspecialidade() {
        return especialidade;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public String getCid10() {
        return cid10;
    }

    public String getPalavrasChave() {
        return palavrasChave;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
