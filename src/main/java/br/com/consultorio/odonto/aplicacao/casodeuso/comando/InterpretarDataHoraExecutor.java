package br.com.consultorio.odonto.aplicacao.casodeuso.comando;

import br.com.consultorio.odonto.aplicacao.casodeuso.interpretacao.InterpretadorPreferenciaHorario;
import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoConversa;
import br.com.consultorio.odonto.aplicacao.porta.entrada.AgendarConsultaUseCase;
import br.com.consultorio.odonto.dominio.modelo.AcaoConversa;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa.InterpretarDataHora;
import br.com.consultorio.odonto.dominio.modelo.Conversa;
import br.com.consultorio.odonto.dominio.modelo.RegrasConversa;

import java.util.Objects;

/** Entende a data/hora pedida pelo paciente e verifica a disponibilidade na agenda. */
public final class InterpretarDataHoraExecutor implements ExecutorComando<InterpretarDataHora> {

    private final InterpretadorPreferenciaHorario interpretador;
    private final AgendarConsultaUseCase agendarConsulta;
    private final ConfiguracaoConversa configuracao;

    public InterpretarDataHoraExecutor(InterpretadorPreferenciaHorario interpretador,
                                       AgendarConsultaUseCase agendarConsulta, ConfiguracaoConversa configuracao) {
        this.interpretador = Objects.requireNonNull(interpretador);
        this.agendarConsulta = Objects.requireNonNull(agendarConsulta);
        this.configuracao = Objects.requireNonNull(configuracao);
    }

    @Override
    public Class<InterpretarDataHora> tipo() {
        return InterpretarDataHora.class;
    }

    @Override
    public AcaoConversa executar(Conversa conversa, InterpretarDataHora comando) {
        RegrasConversa regras = configuracao.regras();
        return interpretador.interpretar(comando.texto())
                .map(preferencia -> conversa.disponibilidadeVerificada(agendarConsulta.verificar(preferencia), regras))
                .orElseGet(() -> conversa.dataHoraNaoEntendida(regras));
    }
}
