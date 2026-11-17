package br.com.consultorio.odonto.adaptador.entrada.webhook;

import br.com.consultorio.odonto.aplicacao.porta.entrada.MensagemRecebidaCommand;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ReceberMensagensUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ReceberMensagensUseCase.ResultadoRecebimento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Webhook da WhatsApp Cloud API.
 * <ul>
 *   <li>GET: desafio de verificação feito pela Meta ao cadastrar a URL.</li>
 *   <li>POST: notificações. Valida a assinatura, grava as mensagens e responde 200 rapidamente; a conversa
 *   é conduzida de forma assíncrona depois do commit.</li>
 * </ul>
 */
@RestController
@RequestMapping("/webhook/whatsapp")
public class WebhookController {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookController.class);

    private final AssinaturaWebhookVerifier verificador;
    private final ConversorWebhook conversor;
    private final ReceberMensagensUseCase receberMensagens;

    public WebhookController(AssinaturaWebhookVerifier verificador, ConversorWebhook conversor,
                             ReceberMensagensUseCase receberMensagens) {
        this.verificador = verificador;
        this.conversor = conversor;
        this.receberMensagens = receberMensagens;
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verificar(@RequestParam(name = "hub.mode", required = false) String modo,
                                            @RequestParam(name = "hub.verify_token", required = false) String token,
                                            @RequestParam(name = "hub.challenge", required = false) String desafio) {
        if ("subscribe".equals(modo) && desafio != null && verificador.tokenVerificacaoValido(token)) {
            return ResponseEntity.ok(desafio);
        }
        LOG.warn("Tentativa de verificação do webhook com token inválido.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping(consumes = MediaType.ALL_VALUE)
    public ResponseEntity<Void> receber(@RequestBody(required = false) byte[] corpo,
                                        @RequestHeader(name = "X-Hub-Signature-256", required = false) String assinatura) {
        if (!verificador.assinaturaValida(corpo, assinatura)) {
            LOG.warn("Webhook recusado: assinatura X-Hub-Signature-256 ausente ou inválida.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<MensagemRecebidaCommand> comandos;
        try {
            comandos = conversor.converter(corpo);
        } catch (IllegalArgumentException e) {
            // Responde 200 mesmo assim: a Meta reenviaria indefinidamente um corpo que nunca será entendido.
            LOG.warn("Webhook com corpo inválido descartado: {}", e.getMessage());
            return ResponseEntity.ok().build();
        }
        if (!comandos.isEmpty()) {
            ResultadoRecebimento resultado = receberMensagens.receber(comandos);
            LOG.debug("Webhook: {} novas, {} duplicadas, {} ignoradas.", resultado.novas(), resultado.duplicadas(),
                    resultado.ignoradas());
        }
        return ResponseEntity.ok().build();
    }
}
