package br.com.consultorio.odonto.adaptador.entrada.lambda;

import br.com.consultorio.odonto.aplicacao.porta.entrada.MensagemRecebidaCommand;
import br.com.consultorio.odonto.dominio.modelo.TipoConteudo;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Contrato (versionado) da mensagem que trafega na fila entre a função do webhook e a função da conversa.
 * É um formato próprio do transporte, independente do comando da aplicação, para que mudanças internas não
 * quebrem mensagens que ainda estejam na fila durante uma implantação.
 */
public record MensagemEnfileirada(int versao, String waMessageId, String waId, String tipo, String texto,
                                  String midiaId, Instant recebidaEm) {

    public static final int VERSAO_ATUAL = 1;

    public MensagemEnfileirada {
        Objects.requireNonNull(waMessageId, "waMessageId");
        Objects.requireNonNull(waId, "waId");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(recebidaEm, "recebidaEm");
    }

    public static MensagemEnfileirada de(MensagemRecebidaCommand comando) {
        return new MensagemEnfileirada(VERSAO_ATUAL, comando.waMessageId(), comando.waId(), comando.tipo().name(),
                comando.texto().orElse(null), comando.midiaId().orElse(null), comando.recebidaEm());
    }

    public MensagemRecebidaCommand paraComando() {
        if (versao != VERSAO_ATUAL) {
            throw new IllegalArgumentException("Versão de mensagem da fila não suportada: " + versao);
        }
        return new MensagemRecebidaCommand(waMessageId, waId, TipoConteudo.valueOf(tipo), Optional.ofNullable(texto),
                Optional.ofNullable(midiaId), recebidaEm);
    }
}
