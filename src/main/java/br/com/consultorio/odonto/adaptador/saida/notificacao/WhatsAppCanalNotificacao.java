package br.com.consultorio.odonto.adaptador.saida.notificacao;

import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoNotificacao;
import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;
import br.com.consultorio.odonto.aplicacao.porta.saida.AlertaConsultorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.AvisoAgendamento;
import br.com.consultorio.odonto.aplicacao.porta.saida.CanalNotificacaoConsultorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.EnviadorWhatsApp;
import br.com.consultorio.odonto.dominio.modelo.WaId;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** Avisa o consultório no WhatsApp do número configurado em {@code WHATSAPP_NUMERO_NOTIFICACAO}. */
@Component
@Order(1)
public class WhatsAppCanalNotificacao implements CanalNotificacaoConsultorio {

    private final EnviadorWhatsApp enviador;
    private final ConfiguracaoNotificacao destino;
    private final ConfiguracaoNotificacaoWhatsApp configuracao;

    public WhatsAppCanalNotificacao(EnviadorWhatsApp enviador, ConfiguracaoNotificacao destino,
                                    ConfiguracaoNotificacaoWhatsApp configuracao) {
        this.enviador = enviador;
        this.destino = destino;
        this.configuracao = configuracao;
    }

    @Override
    public String nome() {
        return "whatsapp";
    }

    @Override
    public boolean habilitado() {
        return destino.numeroNotificacao().isPresent();
    }

    @Override
    public void notificarAgendamento(AvisoAgendamento aviso) {
        enviar(configuracao.templateAgendamento(), aviso.parametrosTemplate(), aviso.texto());
    }

    @Override
    public void notificarAlerta(AlertaConsultorio alerta) {
        enviar(configuracao.templateAlerta(), alerta.parametrosTemplate(), alerta.texto());
    }

    private void enviar(String template, List<String> parametros, String texto) {
        // Mesmo contrato dos demais canais (LSP): chamado sem configuração, falha com FalhaIntegracaoException.
        WaId numero = destino.numeroNotificacao().orElseThrow(
                () -> new FalhaIntegracaoException("Número de notificação do WhatsApp não configurado."));
        if (configuracao.usarTemplate()) {
            enviador.enviarTemplate(numero, template, configuracao.idioma(), parametros);
        } else {
            enviador.enviarTexto(numero, texto);
        }
    }
}
