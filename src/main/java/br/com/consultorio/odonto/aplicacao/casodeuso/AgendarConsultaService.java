package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoAgenda;
import br.com.consultorio.odonto.aplicacao.evento.ConsultaAgendadaEvento;
import br.com.consultorio.odonto.aplicacao.excecao.RecursoNaoEncontradoException;
import br.com.consultorio.odonto.aplicacao.porta.entrada.AgendarConsultaUseCase;
import br.com.consultorio.odonto.aplicacao.porta.saida.BloqueioAgendaRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ConsultaRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ExecutorTransacional;
import br.com.consultorio.odonto.aplicacao.porta.saida.ProntuarioRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.PublicadorEventos;
import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import br.com.consultorio.odonto.dominio.excecao.HorarioIndisponivelException;
import br.com.consultorio.odonto.dominio.modelo.Consulta;
import br.com.consultorio.odonto.dominio.modelo.Disponibilidade;
import br.com.consultorio.odonto.dominio.modelo.Horario;
import br.com.consultorio.odonto.dominio.modelo.PreferenciaHorario;
import br.com.consultorio.odonto.dominio.modelo.Prontuario;
import br.com.consultorio.odonto.dominio.servico.Agenda;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Consulta a disponibilidade e grava consultas. A gravação revalida o horário dentro da transação e conta com
 * o índice único do banco (uma consulta AGENDADA por início) para resolver corridas entre dois pacientes:
 * quem perder recebe {@link ResultadoAgendamento.Indisponivel} com o horário livre mais próximo.
 */
public final class AgendarConsultaService implements AgendarConsultaUseCase {

    private final ConsultaRepositorio consultas;
    private final BloqueioAgendaRepositorio bloqueios;
    private final ProntuarioRepositorio prontuarios;
    private final PublicadorEventos publicador;
    private final ConfiguracaoAgenda configuracao;
    private final ExecutorTransacional transacao;
    private final Relogio relogio;

    public AgendarConsultaService(ConsultaRepositorio consultas, BloqueioAgendaRepositorio bloqueios,
                                  ProntuarioRepositorio prontuarios, PublicadorEventos publicador,
                                  ConfiguracaoAgenda configuracao, ExecutorTransacional transacao, Relogio relogio) {
        this.consultas = Objects.requireNonNull(consultas);
        this.bloqueios = Objects.requireNonNull(bloqueios);
        this.prontuarios = Objects.requireNonNull(prontuarios);
        this.publicador = Objects.requireNonNull(publicador);
        this.configuracao = Objects.requireNonNull(configuracao);
        this.transacao = Objects.requireNonNull(transacao);
        this.relogio = Objects.requireNonNull(relogio);
    }

    @Override
    public Disponibilidade verificar(PreferenciaHorario preferencia) {
        LocalDateTime agora = relogio.agoraLocal();
        return montarAgenda(agora).resolver(preferencia, agora);
    }

    @Override
    public Disponibilidade primeiroHorarioLivre() {
        LocalDateTime agora = relogio.agoraLocal();
        return montarAgenda(agora).primeiroLivre(agora);
    }

    @Override
    public ResultadoAgendamento agendar(AgendarConsultaCommand comando) {
        LocalDateTime agora = relogio.agoraLocal();
        try {
            return transacao.executar(() -> gravar(comando, agora));
        } catch (HorarioIndisponivelException e) {
            PreferenciaHorario mesmoHorario = PreferenciaHorario.de(comando.inicio().toLocalDate(),
                    comando.inicio().toLocalTime(), null);
            return new ResultadoAgendamento.Indisponivel(e.motivo(), montarAgenda(agora).resolver(mesmoHorario, agora));
        }
    }

    private ResultadoAgendamento gravar(AgendarConsultaCommand comando, LocalDateTime agora) {
        Horario horario = montarAgenda(agora).validar(comando.inicio(), agora);
        Consulta consulta = consultas.salvar(
                Consulta.agendar(comando.pacienteId(), comando.queixaId(), horario, relogio.agora()));
        long consultaId = consulta.id().orElseThrow();
        comando.queixaId().ifPresent(queixaId -> vincularQueixa(comando.pacienteId(), queixaId, consultaId));
        if (comando.notificarConsultorio()) {
            publicador.publicar(new ConsultaAgendadaEvento(consultaId));
        }
        return new ResultadoAgendamento.Agendada(consultaId, horario);
    }

    private void vincularQueixa(long pacienteId, long queixaId, long consultaId) {
        Prontuario prontuario = prontuarios.buscarPorPacienteId(pacienteId).orElseThrow(
                () -> new RecursoNaoEncontradoException("Prontuário do paciente " + pacienteId + " não encontrado."));
        prontuario.vincularConsulta(queixaId, consultaId);
        prontuarios.salvar(prontuario);
    }

    /** Monta a agenda com as consultas e bloqueios do período de busca (de hoje até o limite configurado). */
    private Agenda montarAgenda(LocalDateTime agora) {
        LocalDateTime inicio = agora.toLocalDate().atStartOfDay();
        LocalDateTime fim = Agenda.limiteDeBusca(agora, configuracao.diasBuscaMaxima()).plusDays(1).atStartOfDay();
        return new Agenda(configuracao.funcionamento(), configuracao.feriados(),
                bloqueios.listarEntre(inicio, fim), consultas.listarAgendadasEntre(inicio, fim),
                configuracao.diasBuscaMaxima());
    }
}
