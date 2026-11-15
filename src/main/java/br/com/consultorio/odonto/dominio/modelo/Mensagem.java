package br.com.consultorio.odonto.dominio.modelo;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Mensagem de WhatsApp recebida do paciente ou enviada pelo agente.
 */
public final class Mensagem {

    private final Long id;
    private final String waMessageId;
    private final WaId waId;
    private final DirecaoMensagem direcao;
    private final TipoConteudo tipo;
    private final String midiaId;
    private final boolean automatica;
    private final Instant criadaEm;
    private String texto;

    private Mensagem(Long id, String waMessageId, WaId waId, DirecaoMensagem direcao, TipoConteudo tipo, String texto,
                     String midiaId, boolean automatica, Instant criadaEm) {
        this.id = id;
        this.waMessageId = waMessageId;
        this.waId = Objects.requireNonNull(waId, "waId");
        this.direcao = Objects.requireNonNull(direcao, "direcao");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.texto = texto;
        this.midiaId = midiaId;
        this.automatica = automatica;
        this.criadaEm = Objects.requireNonNull(criadaEm, "criadaEm");
    }

    public static Mensagem recebida(String waMessageId, WaId waId, TipoConteudo tipo, Optional<String> texto,
                                    Optional<String> midiaId, Instant recebidaEm) {
        return new Mensagem(null, Objects.requireNonNull(waMessageId, "waMessageId"), waId, DirecaoMensagem.RECEBIDA,
                tipo, texto.orElse(null), midiaId.orElse(null), false, recebidaEm);
    }

    /** Mensagem enviada automaticamente pelo agente. */
    public static Mensagem enviadaPeloAgente(Optional<String> waMessageId, WaId destino, String texto, Instant agora) {
        return new Mensagem(null, waMessageId.orElse(null), destino, DirecaoMensagem.ENVIADA, TipoConteudo.TEXTO, texto,
                null, true, agora);
    }

    public static Mensagem restaurar(Long id, String waMessageId, WaId waId, DirecaoMensagem direcao, TipoConteudo tipo,
                                     String texto, String midiaId, boolean automatica, Instant criadaEm) {
        return new Mensagem(Objects.requireNonNull(id, "id"), waMessageId, waId, direcao, tipo, texto, midiaId,
                automatica, criadaEm);
    }

    /** Guarda o texto transcrito de um áudio. */
    public void registrarTranscricao(String transcricao) {
        if (tipo == TipoConteudo.AUDIO && transcricao != null && !transcricao.isBlank()) {
            this.texto = transcricao.trim();
        }
    }

    public Optional<String> textoParaConversa() {
        return Optional.ofNullable(texto).filter(t -> !t.isBlank());
    }

    public Optional<Long> id() {
        return Optional.ofNullable(id);
    }

    public Optional<String> waMessageId() {
        return Optional.ofNullable(waMessageId);
    }

    public WaId waId() {
        return waId;
    }

    public DirecaoMensagem direcao() {
        return direcao;
    }

    public TipoConteudo tipo() {
        return tipo;
    }

    public Optional<String> texto() {
        return Optional.ofNullable(texto);
    }

    public Optional<String> midiaId() {
        return Optional.ofNullable(midiaId);
    }

    public boolean automatica() {
        return automatica;
    }

    public Instant criadaEm() {
        return criadaEm;
    }
}
