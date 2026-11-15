package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.porta.entrada.IdentificarPacienteUseCase;
import br.com.consultorio.odonto.aplicacao.porta.saida.CifradorDocumento;
import br.com.consultorio.odonto.aplicacao.porta.saida.ConsultaRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ExecutorTransacional;
import br.com.consultorio.odonto.aplicacao.porta.saida.PacienteRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import br.com.consultorio.odonto.dominio.modelo.DocumentoRg;
import br.com.consultorio.odonto.dominio.modelo.Paciente;
import br.com.consultorio.odonto.dominio.modelo.WaId;

import java.util.Objects;
import java.util.Optional;

/**
 * Procura o paciente pelo RG (via hash, pois o RG é gravado cifrado). Se encontrado, vincula o número de
 * WhatsApp ao cadastro e informa a próxima consulta já agendada, se houver.
 */
public final class IdentificarPacienteService implements IdentificarPacienteUseCase {

    private final PacienteRepositorio pacientes;
    private final ConsultaRepositorio consultas;
    private final CifradorDocumento cifrador;
    private final ExecutorTransacional transacao;
    private final Relogio relogio;

    public IdentificarPacienteService(PacienteRepositorio pacientes, ConsultaRepositorio consultas,
                                      CifradorDocumento cifrador, ExecutorTransacional transacao, Relogio relogio) {
        this.pacientes = Objects.requireNonNull(pacientes);
        this.consultas = Objects.requireNonNull(consultas);
        this.cifrador = Objects.requireNonNull(cifrador);
        this.transacao = Objects.requireNonNull(transacao);
        this.relogio = Objects.requireNonNull(relogio);
    }

    @Override
    public Optional<PacienteIdentificado> identificar(DocumentoRg rg, WaId waId) {
        String hash = cifrador.hash(rg);
        return transacao.executar(() -> pacientes.buscarPorRgHash(hash).map(paciente -> identificado(paciente, waId)));
    }

    private PacienteIdentificado identificado(Paciente paciente, WaId waId) {
        if (paciente.vincularTelefone(waId)) {
            paciente = pacientes.salvar(paciente);
        }
        long id = paciente.id().orElseThrow();
        return new PacienteIdentificado(id, paciente.primeiroNome(),
                consultas.proximaAgendadaDoPaciente(id, relogio.agoraLocal()).map(c -> c.horario().inicio()));
    }
}
