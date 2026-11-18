package br.com.consultorio.odonto.adaptador.entrada.lambda;

import br.com.consultorio.odonto.adaptador.entrada.webhook.AssinaturaWebhookVerifier;
import br.com.consultorio.odonto.adaptador.entrada.webhook.ConversorWebhook;
import br.com.consultorio.odonto.aplicacao.porta.entrada.MensagemRecebidaCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Webhook do WhatsApp para execução serverless: mesmas regras do {@code WebhookController} (verificação do
 * token, assinatura {@code X-Hub-Signature-256}, corpo inválido descartado com 200), mas em vez de gravar no
 * banco apenas enfileira as mensagens. Assim a função responde à Meta em milissegundos, sem banco nem IA, e
 * fica fora da VPC.
 */
public class WebhookFilaAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookFilaAdapter.class);

    private final AssinaturaWebhookVerifier verificador;
    private final ConversorWebhook conversor;
    private final FilaMensagensRecebidas fila;

    public WebhookFilaAdapter(AssinaturaWebhookVerifier verificador, ConversorWebhook conversor,
                              FilaMensagensRecebidas fila) {
        this.verificador = Objects.requireNonNull(verificador);
        this.conversor = Objects.requireNonNull(conversor);
        this.fila = Objects.requireNonNull(fila);
    }

    public RespostaHttp tratar(RequisicaoHttp requisicao) {
        return switch (requisicao.metodo()) {
            case "GET" -> verificar(requisicao);
            case "POST" -> receber(requisicao);
            default -> new RespostaHttp(405, "");
        };
    }

    private RespostaHttp verificar(RequisicaoHttp requisicao) {
        String desafio = requisicao.parametro("hub.challenge").orElse(null);
        if ("subscribe".equals(requisicao.parametro("hub.mode").orElse(null)) && desafio != null
                && verificador.tokenVerificacaoValido(requisicao.parametro("hub.verify_token").orElse(null))) {
            return RespostaHttp.ok(desafio);
        }
        LOG.warn("Tentativa de verificação do webhook com token inválido.");
        return new RespostaHttp(403, "");
    }

    private RespostaHttp receber(RequisicaoHttp requisicao) {
        if (!verificador.assinaturaValida(requisicao.corpo(), requisicao.cabecalho("X-Hub-Signature-256").orElse(null))) {
            LOG.warn("Webhook recusado: assinatura X-Hub-Signature-256 ausente ou inválida.");
            return new RespostaHttp(401, "");
        }
        List<MensagemRecebidaCommand> comandos;
        try {
            comandos = conversor.converter(requisicao.corpo());
        } catch (IllegalArgumentException e) {
            // Responde 200 mesmo assim: a Meta reenviaria indefinidamente um corpo que nunca será entendido.
            LOG.warn("Webhook com corpo inválido descartado: {}", e.getMessage());
            return RespostaHttp.ok("");
        }
        if (!comandos.isEmpty()) {
            // Se a fila falhar, a exceção vira HTTP 500 e a Meta reenvia o webhook mais tarde.
            fila.enfileirar(comandos.stream().map(MensagemEnfileirada::de).toList());
            LOG.debug("Webhook: {} mensagens enfileiradas.", comandos.size());
        }
        return RespostaHttp.ok("");
    }
}
