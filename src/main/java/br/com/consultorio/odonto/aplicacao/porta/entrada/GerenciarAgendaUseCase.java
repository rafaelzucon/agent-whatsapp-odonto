package br.com.consultorio.odonto.aplicacao.porta.entrada;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/** Operações da recepção sobre a agenda. */
public interface GerenciarAgendaUseCase {

    /** Agenda do período (no máximo {@value #MAXIMO_DIAS_CONSULTA} dias). */
    AgendaVisao consultar(LocalDate inicio, LocalDate fim);

    /** Agendamento manual pela recepção; lança {@code HorarioIndisponivelException} se o horário não servir. */
    ConsultaVisao agendarManualmente(long pacienteId, Optional<Long> queixaId, LocalDateTime inicio);

    ConsultaVisao cancelar(long consultaId);

    ConsultaVisao marcarRealizada(long consultaId);

    ConsultaVisao marcarFalta(long consultaId);

    BloqueioVisao criarBloqueio(LocalDateTime inicio, LocalDateTime fim, String motivo);

    void removerBloqueio(long bloqueioId);

    int MAXIMO_DIAS_CONSULTA = 92;
}
