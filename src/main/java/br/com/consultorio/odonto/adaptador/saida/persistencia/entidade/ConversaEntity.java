package br.com.consultorio.odonto.adaptador.saida.persistencia.entidade;

import br.com.consultorio.odonto.adaptador.saida.persistencia.cripto.TextoCriptografadoConverter;
import br.com.consultorio.odonto.dominio.modelo.EtapaConversa;
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

/** Estado da conversa de um número de WhatsApp; os dados coletados ficam em JSON cifrado. */
@Entity
@Table(name = "conversa")
public class ConversaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wa_id", nullable = false, unique = true)
    private String waId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EtapaConversa etapa;

    @Convert(converter = TextoCriptografadoConverter.class)
    @Column(name = "dados_cifrados", nullable = false, columnDefinition = "text")
    private String dados;

    @Column(name = "tentativas_invalidas", nullable = false)
    private int tentativasInvalidas;

    @Column(name = "ultima_interacao", nullable = false)
    private Instant ultimaInteracao;

    @Column(name = "iniciada_em", nullable = false)
    private Instant iniciadaEm;

    protected ConversaEntity() {
    }

    public ConversaEntity(Long id, String waId, EtapaConversa etapa, String dados, int tentativasInvalidas,
                          Instant ultimaInteracao, Instant iniciadaEm) {
        this.id = id;
        this.waId = waId;
        this.etapa = etapa;
        this.dados = dados;
        this.tentativasInvalidas = tentativasInvalidas;
        this.ultimaInteracao = ultimaInteracao;
        this.iniciadaEm = iniciadaEm;
    }

    public Long getId() {
        return id;
    }

    public String getWaId() {
        return waId;
    }

    public EtapaConversa getEtapa() {
        return etapa;
    }

    public String getDados() {
        return dados;
    }

    public int getTentativasInvalidas() {
        return tentativasInvalidas;
    }

    public Instant getUltimaInteracao() {
        return ultimaInteracao;
    }

    public Instant getIniciadaEm() {
        return iniciadaEm;
    }
}
