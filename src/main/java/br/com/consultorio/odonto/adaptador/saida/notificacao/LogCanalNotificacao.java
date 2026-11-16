package br.com.consultorio.odonto.adaptador.saida.notificacao;

import br.com.consultorio.odonto.aplicacao.porta.saida.AlertaConsultorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.AvisoAgendamento;
import br.com.consultorio.odonto.aplicacao.porta.saida.CanalNotificacaoConsultorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Registra os avisos no log da aplicação (sempre ativo). Não grava nome nem telefone do paciente,
 * apenas o necessário para rastrear o evento (LGPD).
 */
@Component
@Order(3)
public class LogCanalNotificacao implements CanalNotificacaoConsultorio {

    private static final Logger LOG = LoggerFactory.getLogger(LogCanalNotificacao.class);

    @Override
    public String nome() {
        return "log";
    }

    @Override
    public boolean habilitado() {
        return true;
    }

    @Override
    public void notificarAgendamento(AvisoAgendamento aviso) {
        LOG.info("Nova consulta {} agendada para {} (prontuário {}, prioridade {}).", aviso.consultaId(),
                aviso.dataHoraFormatada(), aviso.numeroProntuario(), aviso.prioridade());
    }

    @Override
    public void notificarAlerta(AlertaConsultorio alerta) {
        LOG.warn("Alerta ao consultório: {} (prioridade {}).", alerta.tipo().descricao(), alerta.prioridade());
    }
}
