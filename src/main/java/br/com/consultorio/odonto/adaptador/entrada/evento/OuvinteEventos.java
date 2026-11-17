package br.com.consultorio.odonto.adaptador.entrada.evento;

import br.com.consultorio.odonto.aplicacao.evento.ConsultaAgendadaEvento;
import br.com.consultorio.odonto.aplicacao.evento.MensagemRecebidaEvento;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ConduzirConversaUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.NotificarConsultorioUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reage aos eventos da aplicação depois do commit, em virtual threads ({@code @Async}): o webhook responde
 * à Meta imediatamente e a conversa segue em segundo plano.
 */
@Component
public class OuvinteEventos {

    private static final Logger LOG = LoggerFactory.getLogger(OuvinteEventos.class);

    private final ConduzirConversaUseCase conduzirConversa;
    private final NotificarConsultorioUseCase notificarConsultorio;
    private final TravasPorConversa travas;

    public OuvinteEventos(ConduzirConversaUseCase conduzirConversa, NotificarConsultorioUseCase notificarConsultorio,
                          TravasPorConversa travas) {
        this.conduzirConversa = conduzirConversa;
        this.notificarConsultorio = notificarConsultorio;
        this.travas = travas;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoReceberMensagem(MensagemRecebidaEvento evento) {
        travas.executar(evento.waId(), () -> {
            try {
                conduzirConversa.conduzir(evento.mensagemId());
            } catch (RuntimeException e) {
                LOG.error("Falha ao conduzir a conversa da mensagem {}.", evento.mensagemId(), e);
            }
        });
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoAgendarConsulta(ConsultaAgendadaEvento evento) {
        try {
            notificarConsultorio.notificarAgendamento(evento.consultaId());
        } catch (RuntimeException e) {
            LOG.error("Falha ao avisar o consultório da consulta {}.", evento.consultaId(), e);
        }
    }
}
