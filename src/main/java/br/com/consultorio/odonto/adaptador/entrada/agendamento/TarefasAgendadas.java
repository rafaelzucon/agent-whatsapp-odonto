package br.com.consultorio.odonto.adaptador.entrada.agendamento;

import br.com.consultorio.odonto.aplicacao.porta.entrada.ExpirarConversasUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ExpurgarMensagensUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ExpurgarMensagensUseCase.ResultadoExpurgo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Rotinas periódicas: expirar conversas paradas e apagar o histórico antigo (retenção LGPD). */
@Component
public class TarefasAgendadas {

    private static final Logger LOG = LoggerFactory.getLogger(TarefasAgendadas.class);

    private final ExpirarConversasUseCase expirarConversas;
    private final ExpurgarMensagensUseCase expurgarMensagens;

    public TarefasAgendadas(ExpirarConversasUseCase expirarConversas, ExpurgarMensagensUseCase expurgarMensagens) {
        this.expirarConversas = expirarConversas;
        this.expurgarMensagens = expurgarMensagens;
    }

    @Scheduled(fixedDelayString = "${odonto.conversa.intervalo-verificacao-expiracao:PT5M}",
            initialDelayString = "PT1M")
    public void expirarConversas() {
        try {
            int expiradas = expirarConversas.expirar();
            if (expiradas > 0) {
                LOG.info("{} conversa(s) expirada(s) por inatividade.", expiradas);
            }
        } catch (RuntimeException e) {
            LOG.error("Falha ao expirar conversas.", e);
        }
    }

    @Scheduled(cron = "${odonto.retencao.cron:0 30 3 * * *}", zone = "${odonto.fuso-horario:America/Sao_Paulo}")
    public void expurgarHistorico() {
        try {
            ResultadoExpurgo resultado = expurgarMensagens.expurgar();
            LOG.info("Retenção: {} mensagem(ns) e {} conversa(s) encerrada(s) removidas.", resultado.mensagens(),
                    resultado.conversas());
        } catch (RuntimeException e) {
            LOG.error("Falha ao aplicar a retenção de mensagens.", e);
        }
    }
}
