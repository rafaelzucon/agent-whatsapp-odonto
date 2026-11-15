package br.com.consultorio.odonto.aplicacao.porta.entrada;

import java.time.LocalDateTime;
import java.util.Optional;

/** Consulta como exibida na API administrativa. */
public record ConsultaVisao(long id, long pacienteId, String nomePaciente, LocalDateTime inicio, LocalDateTime fim,
                            String status, Optional<Long> queixaId, Optional<String> problema,
                            Optional<String> prioridade) {
}
