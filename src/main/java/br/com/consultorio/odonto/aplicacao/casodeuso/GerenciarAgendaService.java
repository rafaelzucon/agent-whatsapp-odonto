package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoAgenda;
import br.com.consultorio.odonto.aplicacao.excecao.RecursoNaoEncontradoException;
import br.com.consultorio.odonto.aplicacao.porta.entrada.AgendaVisao;
import br.com.consultorio.odonto.aplicacao.porta.entrada.AgendarConsultaUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.AgendarConsultaUseCase.AgendarConsultaCommand;
import br.com.consultorio.odonto.aplicacao.porta.entrada.AgendarConsultaUseCase.ResultadoAgendamento;
import br.com.consultorio.odonto.aplicacao.porta.entrada.BloqueioVisao;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ConsultaVisao;
import br.com.consultorio.odonto.aplicacao.porta.entrada.GerenciarAgendaUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.HorarioLivreVisao;
import br.com.consultorio.odonto.aplicacao.porta.saida.BloqueioAgendaRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ConsultaRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ExecutorTransacional;
import br.com.consultorio.odonto.aplicacao.porta.saida.PacienteRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ProntuarioRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;
import br.com.consultorio.odonto.dominio.excecao.HorarioIndisponivelException;
import br.com.consultorio.odonto.dominio.modelo.BloqueioAgenda;
import br.com.consultorio.odonto.dominio.modelo.Consulta;
import br.com.consultorio.odonto.dominio.modelo.Horario;
import br.com.consultorio.odonto.dominio.servico.Agenda;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Operações da recepção sobre a agenda (API administrativa). */
public final class GerenciarAgendaService implements GerenciarAgendaUseCase {

    private final ConsultaRepositorio consultas;
    private final BloqueioAgendaRepositorio bloqueios;
    private final PacienteRepositorio pacientes;
    private final ProntuarioRepositorio prontuarios;
    private final AgendarConsultaUseCase agendarConsulta;
    private final MontadorVisoes visoes;
    private final ConfiguracaoAgenda configuracao;
    private final ExecutorTransacional transacao;
    private final Relogio relogio;

    public GerenciarAgendaService(ConsultaRepositorio consultas, BloqueioAgendaRepositorio bloqueios,
                                  PacienteRepositorio pacientes, ProntuarioRepositorio prontuarios,
                                  AgendarConsultaUseCase agendarConsulta, MontadorVisoes visoes,
                                  ConfiguracaoAgenda configuracao, ExecutorTransacional transacao, Relogio relogio) {
        this.consultas = Objects.requireNonNull(consultas);
        this.bloqueios = Objects.requireNonNull(bloqueios);
        this.pacientes = Objects.requireNonNull(pacientes);
        this.prontuarios = Objects.requireNonNull(prontuarios);
        this.agendarConsulta = Objects.requireNonNull(agendarConsulta);
        this.visoes = Objects.requireNonNull(visoes);
        this.configuracao = Objects.requireNonNull(configuracao);
        this.transacao = Objects.requireNonNull(transacao);
        this.relogio = Objects.requireNonNull(relogio);
    }

    @Override
    public AgendaVisao consultar(LocalDate inicio, LocalDate fim) {
        if (fim.isBefore(inicio)) {
            throw new DadoInvalidoException("A data final deve ser igual ou posterior à inicial.");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) + 1 > MAXIMO_DIAS_CONSULTA) {
            throw new DadoInvalidoException("Consulte no máximo " + MAXIMO_DIAS_CONSULTA + " dias por vez.");
        }
        LocalDateTime de = inicio.atStartOfDay();
        LocalDateTime ate = fim.plusDays(1).atStartOfDay();
        List<Consulta> doPeriodo = consultas.listarEntre(de, ate);
        List<BloqueioAgenda> bloqueiosDoPeriodo = bloqueios.listarEntre(de, ate);
        // Os horários livres respeitam as mesmas regras do agendamento pelo WhatsApp (inclusive o limite de busca).
        Agenda agenda = new Agenda(configuracao.funcionamento(), configuracao.feriados(), bloqueiosDoPeriodo,
                doPeriodo, configuracao.diasBuscaMaxima());
        List<HorarioLivreVisao> livres = agenda.horariosLivres(inicio, fim, relogio.agoraLocal()).stream()
                .map(h -> new HorarioLivreVisao(h.inicio(), h.fim()))
                .toList();
        return new AgendaVisao(inicio, fim, visoes.consultas(doPeriodo), livres,
                bloqueiosDoPeriodo.stream().map(MontadorVisoes::bloqueio).toList());
    }

    @Override
    public ConsultaVisao agendarManualmente(long pacienteId, Optional<Long> queixaId, LocalDateTime inicio) {
        pacientes.buscarPorId(pacienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente " + pacienteId + " não encontrado."));
        queixaId.ifPresent(id -> exigirQueixaDoPaciente(pacienteId, id));
        ResultadoAgendamento resultado = agendarConsulta.agendar(
                new AgendarConsultaCommand(pacienteId, queixaId, inicio, false));
        return switch (resultado) {
            case ResultadoAgendamento.Agendada agendada -> visoes.consulta(buscar(agendada.consultaId()));
            case ResultadoAgendamento.Indisponivel indisponivel ->
                    throw new HorarioIndisponivelException(indisponivel.motivo());
        };
    }

    @Override
    public ConsultaVisao cancelar(long consultaId) {
        return alterar(consultaId, Consulta::cancelar);
    }

    @Override
    public ConsultaVisao marcarRealizada(long consultaId) {
        return alterar(consultaId, c -> c.marcarRealizada(relogio.agoraLocal()));
    }

    @Override
    public ConsultaVisao marcarFalta(long consultaId) {
        return alterar(consultaId, c -> c.marcarFalta(relogio.agoraLocal()));
    }

    @Override
    public BloqueioVisao criarBloqueio(LocalDateTime inicio, LocalDateTime fim, String motivo) {
        BloqueioAgenda bloqueio = BloqueioAgenda.novo(new Horario(inicio, fim), motivo);
        return MontadorVisoes.bloqueio(transacao.executar(() -> bloqueios.salvar(bloqueio)));
    }

    @Override
    public void removerBloqueio(long bloqueioId) {
        if (!transacao.executar(() -> bloqueios.remover(bloqueioId))) {
            throw new RecursoNaoEncontradoException("Bloqueio " + bloqueioId + " não encontrado.");
        }
    }

    private ConsultaVisao alterar(long consultaId, Consumer<Consulta> alteracao) {
        Consulta alterada = transacao.executar(() -> {
            Consulta consulta = buscar(consultaId);
            alteracao.accept(consulta);
            return consultas.salvar(consulta);
        });
        return visoes.consulta(alterada);
    }

    private Consulta buscar(long consultaId) {
        return consultas.buscarPorId(consultaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Consulta " + consultaId + " não encontrada."));
    }

    private void exigirQueixaDoPaciente(long pacienteId, long queixaId) {
        boolean pertence = prontuarios.buscarPorPacienteId(pacienteId)
                .flatMap(p -> p.queixa(queixaId))
                .isPresent();
        if (!pertence) {
            throw new RecursoNaoEncontradoException(
                    "Queixa " + queixaId + " não encontrada no prontuário do paciente " + pacienteId + ".");
        }
    }
}
