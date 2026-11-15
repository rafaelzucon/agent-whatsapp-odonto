package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.excecao.JanelaAtendimentoExpiradaException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Janela de atendimento da Meta: mensagens livres só podem ser enviadas até 24 horas depois
 * da última mensagem recebida do usuário. Fora dela, apenas templates aprovados.
 */
public record JanelaAtendimento(Instant ultimaMensagemDoUsuario) {

    public static final Duration DURACAO = Duration.ofHours(24);

    public JanelaAtendimento {
        Objects.requireNonNull(ultimaMensagemDoUsuario, "ultimaMensagemDoUsuario");
    }

    public boolean estaAberta(Instant agora) {
        return agora.isBefore(ultimaMensagemDoUsuario.plus(DURACAO));
    }

    public void exigirAberta(Instant agora) {
        if (!estaAberta(agora)) {
            throw new JanelaAtendimentoExpiradaException("A janela de 24 horas para respostas livres expirou.");
        }
    }
}
