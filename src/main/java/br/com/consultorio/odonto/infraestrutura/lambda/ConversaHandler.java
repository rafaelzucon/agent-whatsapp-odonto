package br.com.consultorio.odonto.infraestrutura.lambda;

import br.com.consultorio.odonto.adaptador.entrada.lambda.ConsumidorFilaAdapter;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse.BatchItemFailure;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Função da conversa: consome a fila SQS FIFO e conduz a conversa com a aplicação completa (Spring, banco, IA,
 * WhatsApp). Processa as mensagens do lote em ordem, uma por vez, esperando a conversa ser conduzida antes da
 * próxima. Em falha transitória, devolve a mensagem e as seguintes do lote para nova tentativa
 * ({@code ReportBatchItemFailures}), preservando a ordem exigida pela fila FIFO.
 */
public class ConversaHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private static final Logger LOG = LoggerFactory.getLogger(ConversaHandler.class);

    private final ConsumidorFilaAdapter consumidor;
    private final ExecutorAssincronoRastreado executor;

    public ConversaHandler() {
        this(ContextoSpringLambda.obter());
    }

    ConversaHandler(ApplicationContext contexto) {
        this(contexto.getBean(ConsumidorFilaAdapter.class), contexto.getBean(ExecutorAssincronoRastreado.class));
    }

    ConversaHandler(ConsumidorFilaAdapter consumidor, ExecutorAssincronoRastreado executor) {
        this.consumidor = consumidor;
        this.executor = executor;
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent evento, Context contexto) {
        List<BatchItemFailure> falhas = new ArrayList<>();
        List<SQSMessage> registros = evento.getRecords() == null ? List.of() : evento.getRecords();
        for (SQSMessage registro : registros) {
            if (!falhas.isEmpty()) {
                falhas.add(new BatchItemFailure(registro.getMessageId()));
                continue;
            }
            try {
                consumidor.processar(registro.getBody());
                AguardaTarefasAssincronas.aguardar(executor, contexto);
            } catch (RuntimeException e) {
                LOG.error("Falha ao processar a mensagem {} da fila; será tentada de novo.", registro.getMessageId(), e);
                falhas.add(new BatchItemFailure(registro.getMessageId()));
            }
        }
        return SQSBatchResponse.builder().withBatchItemFailures(falhas).build();
    }
}
