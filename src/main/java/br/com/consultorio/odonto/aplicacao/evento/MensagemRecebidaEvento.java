package br.com.consultorio.odonto.aplicacao.evento;

/** Publicado depois que uma mensagem nova do paciente foi gravada; dispara a condução da conversa. */
public record MensagemRecebidaEvento(Long mensagemId, String waId) {
}
