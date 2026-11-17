package br.com.consultorio.odonto.adaptador.entrada.webhook;

import br.com.consultorio.odonto.adaptador.entrada.webhook.WebhookPayload.Mensagem;
import br.com.consultorio.odonto.adaptador.entrada.webhook.WebhookPayload.Valor;
import br.com.consultorio.odonto.aplicacao.porta.entrada.MensagemRecebidaCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Converte o corpo do webhook em comandos de mensagem recebida. Considera só o campo {@code messages}
 * (status de entrega são ignorados) e só mensagens destinadas ao número configurado.
 */
@Component
public class ConversorWebhook {

    private final JsonMapper json;
    private final ConfiguracaoWebhook configuracao;
    private final Clock relogio;

    @Autowired
    public ConversorWebhook(JsonMapper json, ConfiguracaoWebhook configuracao) {
        this(json, configuracao, Clock.systemUTC());
    }

    ConversorWebhook(JsonMapper json, ConfiguracaoWebhook configuracao, Clock relogio) {
        this.json = json;
        this.configuracao = configuracao;
        this.relogio = relogio;
    }

    /** @throws IllegalArgumentException se o corpo não for um JSON válido */
    public List<MensagemRecebidaCommand> converter(byte[] corpo) {
        WebhookPayload payload;
        try {
            payload = json.readValue(corpo, WebhookPayload.class);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Corpo do webhook não é um JSON válido.", e);
        }
        List<MensagemRecebidaCommand> comandos = new ArrayList<>();
        if (payload == null || payload.entry() == null) {
            return comandos;
        }
        payload.entry().stream()
                .filter(Objects::nonNull)
                .flatMap(entrada -> Optional.ofNullable(entrada.changes()).orElse(List.of()).stream())
                .filter(alteracao -> alteracao != null && "messages".equals(alteracao.field()))
                .map(WebhookPayload.Alteracao::value)
                .filter(valor -> valor != null && valor.messages() != null && doNumeroConfigurado(valor))
                .flatMap(valor -> valor.messages().stream())
                .filter(m -> m != null && m.id() != null && m.from() != null)
                .map(this::paraComando)
                .forEach(comandos::add);
        return comandos;
    }

    private boolean doNumeroConfigurado(Valor valor) {
        if (configuracao.phoneNumberId().isEmpty() || valor.metadata() == null) {
            return true;
        }
        return configuracao.phoneNumberId().equals(valor.metadata().phoneNumberId());
    }

    private MensagemRecebidaCommand paraComando(Mensagem m) {
        Instant recebidaEm = instante(m.timestamp());
        String tipo = m.type() == null ? "" : m.type();
        return switch (tipo) {
            case "text" -> MensagemRecebidaCommand.texto(m.id(), m.from(), m.text() == null ? "" : m.text().body(),
                    recebidaEm);
            case "audio" -> m.audio() == null || m.audio().id() == null
                    ? MensagemRecebidaCommand.outro(m.id(), m.from(), recebidaEm)
                    : MensagemRecebidaCommand.audio(m.id(), m.from(), m.audio().id(), recebidaEm);
            case "interactive" -> textoInterativo(m)
                    .map(texto -> MensagemRecebidaCommand.texto(m.id(), m.from(), texto, recebidaEm))
                    .orElseGet(() -> MensagemRecebidaCommand.outro(m.id(), m.from(), recebidaEm));
            case "button" -> m.button() == null || m.button().text() == null
                    ? MensagemRecebidaCommand.outro(m.id(), m.from(), recebidaEm)
                    : MensagemRecebidaCommand.texto(m.id(), m.from(), m.button().text(), recebidaEm);
            default -> MensagemRecebidaCommand.outro(m.id(), m.from(), recebidaEm);
        };
    }

    private static Optional<String> textoInterativo(Mensagem m) {
        if (m.interactive() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(m.interactive().respostaBotao())
                .or(() -> Optional.ofNullable(m.interactive().respostaLista()))
                .map(WebhookPayload.Resposta::title);
    }

    private Instant instante(String timestamp) {
        try {
            return timestamp == null ? relogio.instant() : Instant.ofEpochSecond(Long.parseLong(timestamp));
        } catch (NumberFormatException e) {
            return relogio.instant();
        }
    }
}
