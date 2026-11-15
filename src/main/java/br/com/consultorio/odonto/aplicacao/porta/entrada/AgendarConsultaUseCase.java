package br.com.consultorio.odonto.aplicacao.porta.entrada;

import br.com.consultorio.odonto.dominio.excecao.MotivoIndisponibilidade;
import br.com.consultorio.odonto.dominio.modelo.Disponibilidade;
import br.com.consultorio.odonto.dominio.modelo.Horario;
import br.com.consultorio.odonto.dominio.modelo.PreferenciaHorario;

import java.time.LocalDateTime;
import java.util.Optional;

/** Verifica a disponibilidade na agenda e grava consultas. */
public interface AgendarConsultaUseCase {

    /** Resolve a preferência do paciente: horário disponível, sugestão ou sem horário livre. */
    Disponibilidade verificar(PreferenciaHorario preferencia);

    /** Primeiro horário livre a partir de agora (usado em emergências). */
    Disponibilidade primeiroHorarioLivre();

    /**
     * Grava a consulta revalidando a disponibilidade. Se o horário foi ocupado nesse meio-tempo,
     * devolve {@link ResultadoAgendamento.Indisponivel} com o horário livre mais próximo.
     */
    ResultadoAgendamento agendar(AgendarConsultaCommand comando);

    /**
     * @param notificarConsultorio publica o evento de consulta agendada (aviso à recepção)
     */
    record AgendarConsultaCommand(long pacienteId, Optional<Long> queixaId, LocalDateTime inicio,
                                  boolean notificarConsultorio) {
    }

    sealed interface ResultadoAgendamento {

        record Agendada(long consultaId, Horario horario) implements ResultadoAgendamento {
        }

        record Indisponivel(MotivoIndisponibilidade motivo, Disponibilidade alternativa)
                implements ResultadoAgendamento {
        }
    }
}
