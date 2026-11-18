package br.com.consultorio.odonto.aplicacao.casodeuso.comando;

import br.com.consultorio.odonto.aplicacao.porta.entrada.RegistrarQueixaUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.RegistrarQueixaUseCase.QueixaRegistrada;
import br.com.consultorio.odonto.dominio.modelo.AcaoConversa;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa.RegistrarQueixa;
import br.com.consultorio.odonto.dominio.modelo.Conversa;

import java.util.Objects;

/** Classifica e registra a queixa no prontuário. */
public final class RegistrarQueixaExecutor implements ExecutorComando<RegistrarQueixa> {

    private final RegistrarQueixaUseCase registrarQueixa;

    public RegistrarQueixaExecutor(RegistrarQueixaUseCase registrarQueixa) {
        this.registrarQueixa = Objects.requireNonNull(registrarQueixa);
    }

    @Override
    public Class<RegistrarQueixa> tipo() {
        return RegistrarQueixa.class;
    }

    @Override
    public AcaoConversa executar(Conversa conversa, RegistrarQueixa comando) {
        QueixaRegistrada queixa = registrarQueixa.registrar(comando.pacienteId(), comando.texto());
        return conversa.queixaRegistrada(queixa.queixaId(), queixa.nomeProblema(), queixa.prioridade());
    }
}
