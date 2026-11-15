package br.com.consultorio.odonto.aplicacao.evento;

/** Publicado quando uma consulta é agendada pelo WhatsApp; dispara o aviso ao consultório. */
public record ConsultaAgendadaEvento(Long consultaId) {
}
