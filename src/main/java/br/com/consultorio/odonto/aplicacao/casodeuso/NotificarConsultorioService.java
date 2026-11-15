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

import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Avisa o consultório por todos os canais habilitados (WhatsApp, e-mail, log). A falha de um canal não
 * impede os demais (aberto para novos canais, sem alterar este serviço).
 */
public final class NotificarConsultorioService implements NotificarConsultorioUseCase {

    private static final System.Logger LOG = System.getLogger(NotificarConsultorioService.class.getName());
    static final String PROBLEMA_NAO_INFORMADO = "Não informado";

    private final List<CanalNotificacaoConsultorio> canais;
    private final ConsultaRepositorio consultas;
    private final PacienteRepositorio pacientes;
    private final ProntuarioRepositorio prontuarios;

    public NotificarConsultorioService(List<CanalNotificacaoConsultorio> canais, ConsultaRepositorio consultas,
                                       PacienteRepositorio pacientes, ProntuarioRepositorio prontuarios) {
        this.canais = List.copyOf(canais);
        this.consultas = consultas;
        this.pacientes = pacientes;
        this.prontuarios = prontuarios;
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
        enviar("agendamento da consulta " + consultaId, canal -> canal.notificarAgendamento(aviso));
    }

    @Override
    public void alertar(AlertaConversa alerta) {
        AlertaConsultorio aviso = AlertaConsultorio.de(alerta);
        enviar("alerta " + alerta.tipo(), canal -> canal.notificarAlerta(aviso));
    }

    private void enviar(String descricao, Consumer<CanalNotificacaoConsultorio> envio) {
        int enviados = 0;
        for (CanalNotificacaoConsultorio canal : canais) {
            if (!canal.habilitado()) {
                continue;
            }
            try {
                envio.accept(canal);
                enviados++;
            } catch (RuntimeException e) {
                LOG.log(Level.ERROR, "Falha ao notificar o consultório ({0}) pelo canal {1}: {2}",
                        descricao, canal.nome(), e.getMessage());
            }
        }
        if (enviados == 0) {
            LOG.log(Level.WARNING, "Nenhum canal conseguiu notificar o consultório sobre {0}.", descricao);
        }
    }
}
