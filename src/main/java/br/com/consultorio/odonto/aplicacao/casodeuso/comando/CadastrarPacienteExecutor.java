package br.com.consultorio.odonto.aplicacao.casodeuso.comando;

import br.com.consultorio.odonto.aplicacao.porta.entrada.CadastrarPacienteUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.CadastrarPacienteUseCase.CadastrarPacienteCommand;
import br.com.consultorio.odonto.aplicacao.porta.entrada.CadastrarPacienteUseCase.PacienteCadastrado;
import br.com.consultorio.odonto.dominio.modelo.AcaoConversa;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa.CadastrarPaciente;
import br.com.consultorio.odonto.dominio.modelo.Conversa;

import java.util.Objects;

/** Cadastra o paciente (e abre o prontuário) com os dados coletados na conversa. */
public final class CadastrarPacienteExecutor implements ExecutorComando<CadastrarPaciente> {

    private final CadastrarPacienteUseCase cadastrarPaciente;

    public CadastrarPacienteExecutor(CadastrarPacienteUseCase cadastrarPaciente) {
        this.cadastrarPaciente = Objects.requireNonNull(cadastrarPaciente);
    }

    @Override
    public Class<CadastrarPaciente> tipo() {
        return CadastrarPaciente.class;
    }

    @Override
    public AcaoConversa executar(Conversa conversa, CadastrarPaciente comando) {
        PacienteCadastrado cadastrado = cadastrarPaciente.cadastrar(new CadastrarPacienteCommand(comando.rg(),
                comando.nomeCompleto(), comando.dataNascimento(), comando.responsavelLegal(), conversa.waId()));
        return conversa.pacienteCadastrado(cadastrado.pacienteId(), cadastrado.primeiroNome(),
                cadastrado.numeroProntuario());
    }
}
