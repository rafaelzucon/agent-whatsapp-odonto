package br.com.consultorio.odonto.aplicacao.casodeuso.comando;

import br.com.consultorio.odonto.aplicacao.porta.entrada.IdentificarPacienteUseCase;
import br.com.consultorio.odonto.dominio.modelo.AcaoConversa;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa.IdentificarPaciente;
import br.com.consultorio.odonto.dominio.modelo.Conversa;

import java.util.Objects;

/** Procura o paciente pelo RG informado. */
public final class IdentificarPacienteExecutor implements ExecutorComando<IdentificarPaciente> {

    private final IdentificarPacienteUseCase identificarPaciente;

    public IdentificarPacienteExecutor(IdentificarPacienteUseCase identificarPaciente) {
        this.identificarPaciente = Objects.requireNonNull(identificarPaciente);
    }

    @Override
    public Class<IdentificarPaciente> tipo() {
        return IdentificarPaciente.class;
    }

    @Override
    public AcaoConversa executar(Conversa conversa, IdentificarPaciente comando) {
        return identificarPaciente.identificar(comando.rg(), conversa.waId())
                .map(p -> conversa.pacienteEncontrado(p.pacienteId(), p.primeiroNome(), p.consultaFutura()))
                .orElseGet(conversa::pacienteNaoEncontrado);
    }
}
