package br.com.consultorio.odonto.adaptador.saida.persistencia.entidade;

import br.com.consultorio.odonto.adaptador.saida.persistencia.cripto.TextoCriptografadoConverter;
import br.com.consultorio.odonto.dominio.modelo.DirecaoMensagem;
import br.com.consultorio.odonto.dominio.modelo.TipoConteudo;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Mensagem recebida ou enviada pelo WhatsApp (texto cifrado; expurgada após o prazo de retenção). */
@Entity
@Table(name = "mensagem")
public class MensagemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wa_message_id", unique = true)
    private String waMessageId;

    @Column(name = "wa_id", nullable = false)
    private String waId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DirecaoMensagem direcao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoConteudo tipo;

    @Convert(converter = TextoCriptografadoConverter.class)
    @Column(name = "texto_cifrado", columnDefinition = "text")
    private String texto;

    @Column(name = "midia_id")
    private String midiaId;

    @Column(nullable = false)
    private boolean automatica;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    protected MensagemEntity() {
    }

    public MensagemEntity(Long id, String waMessageId, String waId, DirecaoMensagem direcao, TipoConteudo tipo,
                          String texto, String midiaId, boolean automatica, Instant criadaEm) {
        this.id = id;
        this.waMessageId = waMessageId;
        this.waId = waId;
        this.direcao = direcao;
        this.tipo = tipo;
        this.texto = texto;
        this.midiaId = midiaId;
        this.automatica = automatica;
        this.criadaEm = criadaEm;
    }

    public Long getId() {
        return id;
    }

    public String getWaMessageId() {
        return waMessageId;
    }

    public String getWaId() {
        return waId;
    }

    public DirecaoMensagem getDirecao() {
        return direcao;
    }

    public TipoConteudo getTipo() {
        return tipo;
    }

    public String getTexto() {
        return texto;
    }

    public String getMidiaId() {
        return midiaId;
    }

    public boolean isAutomatica() {
        return automatica;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }
}
