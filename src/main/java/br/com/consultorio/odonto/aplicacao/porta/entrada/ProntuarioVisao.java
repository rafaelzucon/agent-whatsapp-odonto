package br.com.consultorio.odonto.aplicacao.porta.entrada;

import java.time.Instant;
import java.util.List;

public record ProntuarioVisao(String numero, Instant dataAbertura, PacienteVisao paciente, List<QueixaVisao> queixas,
                              List<ConsultaVisao> consultas) {
}
