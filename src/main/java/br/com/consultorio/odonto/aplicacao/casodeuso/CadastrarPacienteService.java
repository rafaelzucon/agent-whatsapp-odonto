package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.porta.entrada.CadastrarPacienteUseCase;
import br.com.consultorio.odonto.aplicacao.porta.saida.CifradorDocumento;
import br.com.consultorio.odonto.aplicacao.porta.saida.ExecutorTransacional;
import br.com.consultorio.odonto.aplicacao.porta.saida.PacienteRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ProntuarioRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import br.com.consultorio.odonto.dominio.modelo.Paciente;
import br.com.consultorio.odonto.dominio.modelo.Prontuario;

import java.time.Instant;
import java.util.Objects;

/**
 * Cadastra o paciente novo e abre o prontuário dele na mesma transação. O comando só é emitido pela conversa
 * depois do consentimento LGPD. Se o RG já existir (ex.: cadastro simultâneo por outro número), o cadastro
 * existente é reaproveitado e o número de WhatsApp é vinculado a ele.
 */
public final class CadastrarPacienteService implements CadastrarPacienteUseCase {

    private final PacienteRepositorio pacientes;
    private final ProntuarioRepositorio prontuarios;
    private final CifradorDocumento cifrador;
    private final ExecutorTransacional transacao;
    private final Relogio relogio;

    public CadastrarPacienteService(PacienteRepositorio pacientes, ProntuarioRepositorio prontuarios,
                                    CifradorDocumento cifrador, ExecutorTransacional transacao, Relogio relogio) {
        this.pacientes = Objects.requireNonNull(pacientes);
        this.prontuarios = Objects.requireNonNull(prontuarios);
        this.cifrador = Objects.requireNonNull(cifrador);
        this.transacao = Objects.requireNonNull(transacao);
        this.relogio = Objects.requireNonNull(relogio);
    }

    @Override
    public PacienteCadastrado cadastrar(CadastrarPacienteCommand comando) {
        String hash = cifrador.hash(comando.rg());
        return transacao.executar(() -> {
            Instant agora = relogio.agora();
            Paciente paciente = pacientes.buscarPorRgHash(hash)
                    .map(existente -> {
                        existente.vincularTelefone(comando.telefone());
                        return existente;
                    })
                    .orElseGet(() -> Paciente.cadastrar(comando.rg(), comando.nomeCompleto(), comando.dataNascimento(),
                            comando.responsavelLegal(), comando.telefone(), true, agora, relogio.hoje()));
            Paciente salvo = pacientes.salvar(paciente);
            long pacienteId = salvo.id().orElseThrow();
            Prontuario prontuario = prontuarios.buscarPorPacienteId(pacienteId)
                    .orElseGet(() -> prontuarios.salvar(
                            Prontuario.abrir(pacienteId, prontuarios.proximoSequencial(), agora)));
            return new PacienteCadastrado(pacienteId, salvo.primeiroNome(), prontuario.numero());
        });
    }
}
