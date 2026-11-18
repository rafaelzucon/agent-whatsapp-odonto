package br.com.consultorio.odonto.adaptador.entrada.lambda;

import br.com.consultorio.odonto.aplicacao.porta.entrada.MensagemRecebidaCommand;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ReceberMensagensUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ReceberMensagensUseCase.ResultadoRecebimento;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsumidorFilaAdapterTest {

    private static final Instant RECEBIDA = Instant.parse("2026-11-16T12:00:00.123Z");

    private final CodecMensagemFila codec = new CodecMensagemFila(JsonMapper.builder().build());
    private final List<MensagemRecebidaCommand> recebidas = new ArrayList<>();
    private final ReceberMensagensUseCase receber = mensagens -> {
        recebidas.addAll(mensagens);
        return new ResultadoRecebimento(mensagens.size(), 0, 0);
    };
    private final ConsumidorFilaAdapter consumidor = new ConsumidorFilaAdapter(codec, receber);

    @Test
    void entregaAoCasoDeUsoOMesmoComandoQueOWebhookEnfileirou() {
        MensagemRecebidaCommand texto = MensagemRecebidaCommand.texto("wamid.1", "5511999990000", "Olá, dói 🦷", RECEBIDA);
        MensagemRecebidaCommand audio = MensagemRecebidaCommand.audio("wamid.2", "5511999990000", "MIDIA", RECEBIDA);

        assertThat(consumidor.processar(codec.paraJson(MensagemEnfileirada.de(texto)))).isTrue();
        assertThat(consumidor.processar(codec.paraJson(MensagemEnfileirada.de(audio)))).isTrue();

        assertThat(recebidas).containsExactly(texto, audio);
    }

    @Test
    void descartaCorpoInvalidoOuDeVersaoDesconhecida() {
        String versaoFutura = codec.paraJson(new MensagemEnfileirada(99, "w", "5511", "TEXTO", "x", null, RECEBIDA));

        assertThat(consumidor.processar("{quebrado")).isFalse();
        assertThat(consumidor.processar("null")).isFalse();
        assertThat(consumidor.processar(versaoFutura)).isFalse();
        assertThat(recebidas).isEmpty();
    }

    @Test
    void falhaTransitoriaPropagaParaAFilaReentregar() {
        ConsumidorFilaAdapter comBancoFora = new ConsumidorFilaAdapter(codec, mensagens -> {
            throw new IllegalStateException("banco fora do ar");
        });
        String corpo = codec.paraJson(MensagemEnfileirada.de(
                MensagemRecebidaCommand.outro("wamid.3", "5511999990000", RECEBIDA)));

        assertThatThrownBy(() -> comBancoFora.processar(corpo)).hasMessage("banco fora do ar");
    }
}
