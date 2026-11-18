package br.com.consultorio.odonto.adaptador.entrada.lambda;

import br.com.consultorio.odonto.aplicacao.porta.entrada.ReceberMensagensUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ReceberMensagensUseCase.ResultadoRecebimento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Consome uma mensagem da fila e a entrega ao caso de uso de recebimento, que deduplica pelo id da Meta,
 * grava e dispara a condução da conversa. Reprocessar a mesma mensagem é seguro (vira duplicata).
 */
public class ConsumidorFilaAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumidorFilaAdapter.class);

    private final CodecMensagemFila codec;
    private final ReceberMensagensUseCase receberMensagens;

    public ConsumidorFilaAdapter(CodecMensagemFila codec, ReceberMensagensUseCase receberMensagens) {
        this.codec = Objects.requireNonNull(codec);
        this.receberMensagens = Objects.requireNonNull(receberMensagens);
    }

    /**
     * @return {@code false} se o corpo é inválido e deve ser descartado (não adianta tentar de novo)
     * @throws RuntimeException em falhas transitórias (banco fora do ar etc.): a fila deve reentregar
     */
    public boolean processar(String corpo) {
        MensagemEnfileirada mensagem;
        try {
            mensagem = codec.deJson(corpo);
            ResultadoRecebimento resultado = receberMensagens.receber(List.of(mensagem.paraComando()));
            LOG.debug("Fila: {} nova(s), {} duplicada(s), {} ignorada(s).", resultado.novas(),
                    resultado.duplicadas(), resultado.ignoradas());
            return true;
        } catch (IllegalArgumentException e) {
            LOG.error("Mensagem da fila descartada por ser inválida: {}", e.getMessage());
            return false;
        }
    }
}
