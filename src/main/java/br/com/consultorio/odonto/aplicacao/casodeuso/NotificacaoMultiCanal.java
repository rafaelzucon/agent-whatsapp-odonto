package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.porta.saida.AlertaConsultorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.AvisoAgendamento;
import br.com.consultorio.odonto.aplicacao.porta.saida.CanalNotificacaoConsultorio;

import java.lang.System.Logger.Level;
import java.util.List;
import java.util.function.Consumer;

/**
 * Composite que se comporta como um único {@link CanalNotificacaoConsultorio}: repassa o aviso a todos os canais
 * habilitados (WhatsApp, e-mail, log...) e isola a falha de cada um, para que os demais continuem. Novos canais
 * entram só na composição, sem alterar este código nem o caso de uso (OCP).
 */
public final class NotificacaoMultiCanal implements CanalNotificacaoConsultorio {

    private static final System.Logger LOG = System.getLogger(NotificacaoMultiCanal.class.getName());

    private final List<CanalNotificacaoConsultorio> canais;

    public NotificacaoMultiCanal(List<CanalNotificacaoConsultorio> canais) {
        this.canais = List.copyOf(canais);
    }

    @Override
    public String nome() {
        return "multicanal";
    }

    @Override
    public boolean habilitado() {
        return canais.stream().anyMatch(CanalNotificacaoConsultorio::habilitado);
    }

    @Override
    public void notificarAgendamento(AvisoAgendamento aviso) {
        enviar("agendamento da consulta " + aviso.consultaId(), canal -> canal.notificarAgendamento(aviso));
    }

    @Override
    public void notificarAlerta(AlertaConsultorio alerta) {
        enviar("alerta " + alerta.tipo(), canal -> canal.notificarAlerta(alerta));
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
