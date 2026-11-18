package br.com.consultorio.odonto.aplicacao.casodeuso.comando;

import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoConversa;
import br.com.consultorio.odonto.aplicacao.porta.entrada.AgendarConsultaUseCase;
import br.com.consultorio.odonto.dominio.modelo.AcaoConversa;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa.BuscarPrimeiroHorarioLivre;
import br.com.consultorio.odonto.dominio.modelo.Conversa;

import java.util.Objects;

/** Sugere o primeiro horário livre da agenda (paciente com dor que quer "o quanto antes"). */
public final class BuscarPrimeiroHorarioLivreExecutor implements ExecutorComando<BuscarPrimeiroHorarioLivre> {

    private final AgendarConsultaUseCase agendarConsulta;
    private final ConfiguracaoConversa configuracao;

    public BuscarPrimeiroHorarioLivreExecutor(AgendarConsultaUseCase agendarConsulta,
                                              ConfiguracaoConversa configuracao) {
        this.agendarConsulta = Objects.requireNonNull(agendarConsulta);
        this.configuracao = Objects.requireNonNull(configuracao);
    }

    @Override
    public Class<BuscarPrimeiroHorarioLivre> tipo() {
        return BuscarPrimeiroHorarioLivre.class;
    }

    @Override
    public AcaoConversa executar(Conversa conversa, BuscarPrimeiroHorarioLivre comando) {
        return conversa.disponibilidadeVerificada(agendarConsulta.primeiroHorarioLivre(), configuracao.regras());
    }
}
