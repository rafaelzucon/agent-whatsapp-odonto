package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.evento.MensagemRecebidaEvento;
import br.com.consultorio.odonto.aplicacao.porta.entrada.MensagemRecebidaCommand;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ReceberMensagensUseCase.ResultadoRecebimento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReceberMensagensServiceTest {

    private final Cenario cenario = new Cenario();

    private MensagemRecebidaCommand texto(String wamid, String waId) {
        return MensagemRecebidaCommand.texto(wamid, waId, "Olá", cenario.relogio.agora());
    }

    @Test
    @DisplayName("reentrega da Meta com o mesmo wamid não gera novo processamento")
    void ignoraDuplicadas() {
        cenario.receber.receber(List.of(texto("wamid.1", Cenario.PACIENTE)));

        ResultadoRecebimento resultado = cenario.receber.receber(
                List.of(texto("wamid.1", Cenario.PACIENTE), texto("wamid.2", Cenario.PACIENTE)));

        assertThat(resultado).isEqualTo(new ResultadoRecebimento(1, 1, 0));
        assertThat(cenario.publicador.eventos(MensagemRecebidaEvento.class)).hasSize(2);
        assertThat(cenario.mensagens.porId).hasSize(2);
    }

    @Test
    @DisplayName("ignora mensagens do próprio número do consultório e remetentes inválidos")
    void ignoraConsultorioEInvalidos() {
        ResultadoRecebimento resultado = cenario.receber.receber(
                List.of(texto("wamid.1", Cenario.CONSULTORIO), texto("wamid.2", "abc")));

        assertThat(resultado).isEqualTo(new ResultadoRecebimento(0, 0, 2));
        assertThat(cenario.publicador.eventos).isEmpty();
        assertThat(cenario.mensagens.porId).isEmpty();
    }
}
