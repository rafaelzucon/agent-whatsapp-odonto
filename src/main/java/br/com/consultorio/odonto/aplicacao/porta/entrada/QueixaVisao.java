package br.com.consultorio.odonto.aplicacao.porta.entrada;

import java.time.Instant;
import java.util.Optional;

public record QueixaVisao(long id, String texto, String codigoProblema, String nomeProblema, String prioridade,
                          double confianca, String origem, Instant dataRegistro, Optional<Long> consultaId) {
}
