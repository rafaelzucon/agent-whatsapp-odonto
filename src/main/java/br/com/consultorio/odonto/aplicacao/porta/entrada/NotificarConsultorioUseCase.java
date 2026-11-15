package br.com.consultorio.odonto.aplicacao.porta.entrada;

import br.com.consultorio.odonto.dominio.modelo.AlertaConversa;

/** Avisa o consultório por todos os canais habilitados; a falha de um canal não impede os outros. */
public interface NotificarConsultorioUseCase {

    /** Nome do paciente, data e horário, problema, prioridade e número do prontuário. */
    void notificarAgendamento(long consultaId);

    /** Emergência, consentimento recusado ou atendimento humano necessário. */
    void alertar(AlertaConversa alerta);
}
