package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.porta.entrada.BloqueioVisao;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ConsultaVisao;
import br.com.consultorio.odonto.aplicacao.porta.entrada.PacienteVisao;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ProblemaVisao;
import br.com.consultorio.odonto.aplicacao.porta.entrada.QueixaVisao;
import br.com.consultorio.odonto.aplicacao.porta.saida.PacienteRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ProntuarioRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import br.com.consultorio.odonto.dominio.modelo.BloqueioAgenda;
import br.com.consultorio.odonto.dominio.modelo.Consulta;
import br.com.consultorio.odonto.dominio.modelo.Paciente;
import br.com.consultorio.odonto.dominio.modelo.ProblemaOdontologico;
import br.com.consultorio.odonto.dominio.modelo.Prontuario;
import br.com.consultorio.odonto.dominio.modelo.QueixaRelatada;
import br.com.consultorio.odonto.dominio.modelo.WaId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Converte objetos de domínio nas visões devolvidas pela API administrativa. */
public final class MontadorVisoes {

    private final PacienteRepositorio pacientes;
    private final ProntuarioRepositorio prontuarios;
    private final Relogio relogio;

    public MontadorVisoes(PacienteRepositorio pacientes, ProntuarioRepositorio prontuarios, Relogio relogio) {
        this.pacientes = Objects.requireNonNull(pacientes);
        this.prontuarios = Objects.requireNonNull(prontuarios);
        this.relogio = Objects.requireNonNull(relogio);
    }

    public List<ConsultaVisao> consultas(List<Consulta> consultas) {
        Map<Long, Optional<Paciente>> cachePacientes = new HashMap<>();
        Map<Long, Optional<Prontuario>> cacheProntuarios = new HashMap<>();
        return consultas.stream()
                .map(c -> consulta(c,
                        cachePacientes.computeIfAbsent(c.pacienteId(), pacientes::buscarPorId),
                        cacheProntuarios.computeIfAbsent(c.pacienteId(), prontuarios::buscarPorPacienteId)))
                .toList();
    }

    public ConsultaVisao consulta(Consulta consulta) {
        return consultas(List.of(consulta)).getFirst();
    }

    private static ConsultaVisao consulta(Consulta consulta, Optional<Paciente> paciente,
                                          Optional<Prontuario> prontuario) {
        Optional<QueixaRelatada> queixa = consulta.queixaId().flatMap(id -> prontuario.flatMap(p -> p.queixa(id)));
        return new ConsultaVisao(consulta.id().orElseThrow(), consulta.pacienteId(),
                paciente.map(Paciente::nomeCompleto).orElse("?"), consulta.horario().inicio(),
                consulta.horario().fim(), consulta.status().name(), consulta.queixaId(),
                queixa.map(q -> q.problema().nome()), queixa.map(q -> q.prioridade().name()));
    }

    public PacienteVisao paciente(Paciente paciente, Optional<Prontuario> prontuario) {
        return new PacienteVisao(paciente.id().orElseThrow(), paciente.nomeCompleto(), paciente.rg().numero(),
                paciente.rg().uf().name(), paciente.dataNascimento(), paciente.idade(relogio.hoje()),
                paciente.responsavelLegal(), paciente.telefones().stream().map(WaId::valor).sorted().toList(),
                prontuario.map(Prontuario::numero), paciente.consentimentoLgpd(), paciente.dataConsentimento(),
                paciente.dataCadastro());
    }

    public PacienteVisao paciente(Paciente paciente) {
        return paciente(paciente, prontuarios.buscarPorPacienteId(paciente.id().orElseThrow()));
    }

    public static QueixaVisao queixa(QueixaRelatada queixa) {
        return new QueixaVisao(queixa.id().orElseThrow(), queixa.textoOriginal(), queixa.problema().codigo(),
                queixa.problema().nome(), queixa.prioridade().name(), queixa.confianca(), queixa.origem().name(),
                queixa.dataRegistro(), queixa.consultaId());
    }

    public static ProblemaVisao problema(ProblemaOdontologico problema) {
        return new ProblemaVisao(problema.codigo(), problema.nome(), problema.descricao(), problema.especialidade(),
                problema.prioridade().name(), problema.cid10(), problema.palavrasChave());
    }

    public static BloqueioVisao bloqueio(BloqueioAgenda bloqueio) {
        return new BloqueioVisao(bloqueio.id().orElseThrow(), bloqueio.periodo().inicio(), bloqueio.periodo().fim(),
                bloqueio.motivo());
    }
}
