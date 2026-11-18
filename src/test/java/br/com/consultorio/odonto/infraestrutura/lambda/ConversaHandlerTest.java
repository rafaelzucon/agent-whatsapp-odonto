package br.com.consultorio.odonto.infraestrutura.lambda;

import br.com.consultorio.odonto.adaptador.entrada.lambda.CodecMensagemFila;
import br.com.consultorio.odonto.adaptador.entrada.lambda.ConsumidorFilaAdapter;
import br.com.consultorio.odonto.adaptador.entrada.lambda.MensagemEnfileirada;
import br.com.consultorio.odonto.aplicacao.porta.entrada.MensagemRecebidaCommand;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ReceberMensagensUseCase.ResultadoRecebimento;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse.BatchItemFailure;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConversaHandlerTest {

    private final CodecMensagemFila codec = new CodecMensagemFila(JsonMapper.builder().build());
    private final ExecutorAssincronoRastreado executor = new ExecutorAssincronoRastreado();
    private final List<String> conduzidas = new ArrayList<>();

    private ConversaHandler handler(String idQueFalha) {
        ConsumidorFilaAdapter consumidor = new ConsumidorFilaAdapter(codec, mensagens -> {
            MensagemRecebidaCommand comando = mensagens.getFirst();
            if (comando.waMessageId().equals(idQueFalha)) {
                throw new IllegalStateException("banco fora do ar");
            }
            // Simula o ouvinte @Async que conduz a conversa depois do commit.
            executor.execute(() -> {
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                synchronized (conduzidas) {
                    conduzidas.add(comando.waMessageId());
                }
            });
            return new ResultadoRecebimento(1, 0, 0);
        });
        return new ConversaHandler(consumidor, executor);
    }

    private SQSMessage registro(String idSqs, String waMessageId) {
        SQSMessage mensagem = new SQSMessage();
        mensagem.setMessageId(idSqs);
        mensagem.setBody(codec.paraJson(MensagemEnfileirada.de(MensagemRecebidaCommand.texto(waMessageId,
                "5511999990000", "oi", Instant.parse("2026-11-16T12:00:00Z")))));
        return mensagem;
    }

    private SQSEvent evento(SQSMessage... registros) {
        SQSEvent evento = new SQSEvent();
        evento.setRecords(List.of(registros));
        return evento;
    }

    @Test
    void conduzCadaMensagemAteOFimAntesDaProxima() {
        SQSBatchResponse resposta = handler("nenhuma").handleRequest(
                evento(registro("s1", "w1"), registro("s2", "w2")), null);

        assertThat(resposta.getBatchItemFailures()).isEmpty();
        assertThat(conduzidas).containsExactly("w1", "w2");
    }

    @Test
    void emFalhaDevolveAMensagemEAsSeguintesParaPreservarAOrdem() {
        SQSMessage invalida = new SQSMessage();
        invalida.setMessageId("s0");
        invalida.setBody("lixo");

        SQSBatchResponse resposta = handler("w2").handleRequest(
                evento(invalida, registro("s1", "w1"), registro("s2", "w2"), registro("s3", "w3")), null);

        assertThat(resposta.getBatchItemFailures()).extracting(BatchItemFailure::getItemIdentifier)
                .containsExactly("s2", "s3");
        assertThat(conduzidas).containsExactly("w1");
    }
}
