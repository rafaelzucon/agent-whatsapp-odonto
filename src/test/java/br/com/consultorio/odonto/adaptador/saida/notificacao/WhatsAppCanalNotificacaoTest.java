package br.com.consultorio.odonto.adaptador.saida.notificacao;

import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoNotificacao;
import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;
import br.com.consultorio.odonto.aplicacao.porta.saida.AlertaConsultorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.EnviadorWhatsApp;
import br.com.consultorio.odonto.dominio.modelo.Prioridade;
import br.com.consultorio.odonto.dominio.modelo.TipoAlerta;
import br.com.consultorio.odonto.dominio.modelo.WaId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhatsAppCanalNotificacaoTest {

    @Test
    @DisplayName("Sem número configurado, falha como os demais canais: FalhaIntegracaoException (LSP)")
    void semNumeroFalhaComExcecaoDoContrato() {
        EnviadorWhatsApp enviador = new EnviadorWhatsApp() {
            @Override
            public String enviarTexto(WaId destino, String texto) {
                throw new AssertionError("não deveria enviar");
            }

            @Override
            public String enviarTemplate(WaId destino, String nome, String idioma, List<String> parametros) {
                throw new AssertionError("não deveria enviar");
            }
        };
        WhatsAppCanalNotificacao canal = new WhatsAppCanalNotificacao(enviador,
                new ConfiguracaoNotificacao(Optional.empty()),
                new ConfiguracaoNotificacaoWhatsApp(true, "agendamento", "alerta", "pt_BR"));

        assertThatThrownBy(() -> canal.notificarAlerta(new AlertaConsultorio(TipoAlerta.values()[0], "Paciente",
                "resumo", Prioridade.ROTINA)))
                .isInstanceOf(FalhaIntegracaoException.class);
    }
}
