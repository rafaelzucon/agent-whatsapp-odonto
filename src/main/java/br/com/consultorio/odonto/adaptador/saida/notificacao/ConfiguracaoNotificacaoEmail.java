package br.com.consultorio.odonto.adaptador.saida.notificacao;

import java.util.List;

/**
 * Aviso por e-mail (opcional). Fica ativo quando há destinatários e o SMTP está configurado
 * ({@code spring.mail.host}).
 */
public record ConfiguracaoNotificacaoEmail(String remetente, List<String> destinatarios) {

    public ConfiguracaoNotificacaoEmail {
        remetente = remetente == null ? "" : remetente.strip();
        destinatarios = destinatarios == null ? List.of()
                : destinatarios.stream().map(String::strip).filter(d -> !d.isEmpty()).toList();
    }
}
