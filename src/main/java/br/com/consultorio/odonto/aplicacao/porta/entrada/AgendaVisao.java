package br.com.consultorio.odonto.aplicacao.porta.entrada;

import java.time.LocalDate;
import java.util.List;

/** Agenda de um período: consultas (todos os status), horários livres e bloqueios. */
public record AgendaVisao(LocalDate inicio, LocalDate fim, List<ConsultaVisao> consultas,
                          List<HorarioLivreVisao> horariosLivres, List<BloqueioVisao> bloqueios) {
}
