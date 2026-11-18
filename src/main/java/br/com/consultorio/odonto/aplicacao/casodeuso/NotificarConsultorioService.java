package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.excecao.RecursoNaoEncontradoException;
import br.com.consultorio.odonto.aplicacao.porta.entrada.NotificarConsultorioUseCase;
import br.com.consultorio.odonto.aplicacao.porta.saida.AlertaConsultorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.AvisoAgendamento;
import br.com.consultorio.odonto.aplicacao.porta.saida.CanalNotificacaoConsultorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ConsultaRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.PacienteRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ProntuarioRepositorio;
import br.com.consultorio.odonto.dominio.modelo.AlertaConversa;
import br.com.consultorio.odonto.dominio.modelo.Consulta;
import br.com.consultorio.odonto.dominio.modelo.Paciente;
import br.com.consultorio.odonto.dominio.modelo.Prioridade;
import br.com.consultorio.odonto.dominio.modelo.Prontuario;
import br.com.consultorio.odonto.dominio.modelo.QueixaRelatada;

import java.util.Objects;
import java.util.Optional;

/**
 * Monta o aviso de agendamento ou o alerta e o entrega ao {@link CanalNotificacaoConsultorio}. Quantos canais
 * existem e como as falhas são isoladas é responsabilidade do canal recebido (por padrão, o composite
 * {@link NotificacaoMultiCanal}), não deste serviço (SRP/DIP).
 */
public final class NotificarConsultorioService implements NotificarConsultorioUseCase {

    static final String PROBLEMA_NAO_INFORMADO = "Não informado";

    private final CanalNotificacaoConsultorio canal;
    private final ConsultaRepositorio consultas;
    private final PacienteRepositorio pacientes;
    private final ProntuarioRepositorio prontuarios;

    public NotificarConsultorioService(CanalNotificacaoConsultorio canal, ConsultaRepositorio consultas,
                                       PacienteRepositorio pacientes, ProntuarioRepositorio prontuarios) {
        this.canal = Objects.requireNonNull(canal);
        this.consultas = Objects.requireNonNull(consultas);
        this.pacientes = Objects.requireNonNull(pacientes);
        this.prontuarios = Objects.requireNonNull(prontuarios);
    }

    @Override
    public void notificarAgendamento(long consultaId) {
        Consulta consulta = consultas.buscarPorId(consultaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Consulta " + consultaId + " não encontrada."));
        Paciente paciente = pacientes.buscarPorId(consulta.pacienteId()).orElseThrow(
                () -> new RecursoNaoEncontradoException("Paciente " + consulta.pacienteId() + " não encontrado."));
        Optional<Prontuario> prontuario = prontuarios.buscarPorPacienteId(consulta.pacienteId());
        Optional<QueixaRelatada> queixa = consulta.queixaId().flatMap(id -> prontuario.flatMap(p -> p.queixa(id)));
        AvisoAgendamento aviso = new AvisoAgendamento(consultaId, paciente.nomeCompleto(), consulta.horario().inicio(),
                queixa.map(q -> q.problema().nome()).orElse(PROBLEMA_NAO_INFORMADO),
                queixa.map(QueixaRelatada::prioridade).orElse(Prioridade.ROTINA),
                prontuario.map(Prontuario::numero).orElse("-"));
        canal.notificarAgendamento(aviso);
    }

    @Override
    public void alertar(AlertaConversa alerta) {
        canal.notificarAlerta(AlertaConsultorio.de(alerta));
    }
}
