package br.com.consultorio.odonto.aplicacao.porta.entrada;

import java.time.LocalDateTime;

public record BloqueioVisao(long id, LocalDateTime inicio, LocalDateTime fim, String motivo) {
}
