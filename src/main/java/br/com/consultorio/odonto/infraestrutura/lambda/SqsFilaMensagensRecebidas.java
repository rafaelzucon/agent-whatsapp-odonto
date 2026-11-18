package br.com.consultorio.odonto.infraestrutura.lambda;

import br.com.consultorio.odonto.adaptador.entrada.lambda.CodecMensagemFila;
import br.com.consultorio.odonto.adaptador.entrada.lambda.FilaMensagensRecebidas;
import br.com.consultorio.odonto.adaptador.entrada.lambda.MensagemEnfileirada;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Fila SQS FIFO: grupo de mensagens = número do paciente (preserva a ordem da conversa e permite processar
 * pacientes diferentes em paralelo) e id de deduplicação = id da mensagem na Meta (descarta reenvios do
 * webhook dentro da janela de 5 minutos; fora dela, o caso de uso também deduplica).
 */
public class SqsFilaMensagensRecebidas implements FilaMensagensRecebidas {

    static final int TAMANHO_MAXIMO_LOTE = 10;

    private final SqsClient sqs;
    private final String urlFila;
    private final CodecMensagemFila codec;

    public SqsFilaMensagensRecebidas(SqsClient sqs, String urlFila, CodecMensagemFila codec) {
        this.sqs = Objects.requireNonNull(sqs);
        if (urlFila == null || urlFila.isBlank()) {
            throw new IllegalStateException("Informe a URL da fila em FILA_MENSAGENS_URL.");
        }
        this.urlFila = urlFila;
        this.codec = Objects.requireNonNull(codec);
    }

    @Override
    public void enfileirar(List<MensagemEnfileirada> mensagens) {
        for (int inicio = 0; inicio < mensagens.size(); inicio += TAMANHO_MAXIMO_LOTE) {
            enviarLote(mensagens.subList(inicio, Math.min(inicio + TAMANHO_MAXIMO_LOTE, mensagens.size())));
        }
    }

    private void enviarLote(List<MensagemEnfileirada> lote) {
        List<SendMessageBatchRequestEntry> entradas = new ArrayList<>();
        for (int i = 0; i < lote.size(); i++) {
            MensagemEnfileirada mensagem = lote.get(i);
            entradas.add(SendMessageBatchRequestEntry.builder()
                    .id(Integer.toString(i))
                    .messageBody(codec.paraJson(mensagem))
                    .messageGroupId(mensagem.waId())
                    .messageDeduplicationId(idDeduplicacao(mensagem.waMessageId()))
                    .build());
        }
        SendMessageBatchResponse resposta = sqs.sendMessageBatch(r -> r.queueUrl(urlFila).entries(entradas));
        if (resposta.hasFailed() && !resposta.failed().isEmpty()) {
            throw new IllegalStateException("Falha ao enfileirar mensagens: " + resposta.failed().stream()
                    .map(BatchResultErrorEntry::message).collect(Collectors.joining("; ")));
        }
    }

    /** O SQS aceita até 128 caracteres alfanuméricos e de pontuação; os ids da Meta ("wamid.") cabem. */
    static String idDeduplicacao(String waMessageId) {
        String limpo = waMessageId.replaceAll("[^\\p{Alnum}\\p{Punct}]", "");
        return limpo.length() <= 128 ? limpo : limpo.substring(limpo.length() - 128);
    }
}
