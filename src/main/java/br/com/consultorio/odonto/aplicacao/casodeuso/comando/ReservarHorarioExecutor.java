package br.com.consultorio.odonto.aplicacao.casodeuso.comando;

import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoConversa;
import br.com.consultorio.odonto.aplicacao.porta.entrada.AgendarConsultaUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.AgendarConsultaUseCase.AgendarConsultaCommand;
import br.com.consultorio.odonto.aplicacao.porta.entrada.AgendarConsultaUseCase.ResultadoAgendamento;
import br.com.consultorio.odonto.dominio.modelo.AcaoConversa;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa.ReservarHorario;
import br.com.consultorio.odonto.dominio.modelo.Conversa;

import java.util.Objects;

/** Agenda a consulta; se o horário foi ocupado nesse meio-tempo, devolve a alternativa à conversa. */
public final class ReservarHorarioExecutor implements ExecutorComando<ReservarHorario> {

    private final AgendarConsultaUseCase agendarConsulta;
    private final ConfiguracaoConversa configuracao;

    public ReservarHorarioExecutor(AgendarConsultaUseCase agendarConsulta, ConfiguracaoConversa configuracao) {
        this.agendarConsulta = Objects.requireNonNull(agendarConsulta);
        this.configuracao = Objects.requireNonNull(configuracao);
    }

    @Override
    public Class<ReservarHorario> tipo() {
        return ReservarHorario.class;
    }

    @Override
    public AcaoConversa executar(Conversa conversa, ReservarHorario comando) {
        ResultadoAgendamento resultado = agendarConsulta.agendar(new AgendarConsultaCommand(comando.pacienteId(),
                comando.queixaId(), comando.horario().inicio(), true));
        return switch (resultado) {
            case ResultadoAgendamento.Agendada agendada -> conversa.consultaAgendada(agendada.horario());
            case ResultadoAgendamento.Indisponivel indisponivel ->
                    conversa.disponibilidadeVerificada(indisponivel.alternativa(), configuracao.regras());
        };
    }
}
