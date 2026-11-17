package br.com.consultorio.odonto.adaptador.entrada.webhook;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Confere o cabeçalho {@code X-Hub-Signature-256} (HMAC-SHA256 do corpo bruto com o App Secret), garantindo
 * que a notificação veio da Meta e não foi alterada. A comparação é feita em tempo constante.
 */
@Component
public class AssinaturaWebhookVerifier {

    private static final String PREFIXO = "sha256=";
    private static final String ALGORITMO = "HmacSHA256";

    private final ConfiguracaoWebhook configuracao;

    public AssinaturaWebhookVerifier(ConfiguracaoWebhook configuracao) {
        this.configuracao = configuracao;
    }

    public boolean assinaturaValida(byte[] corpo, String cabecalho) {
        if (configuracao.segredoApp().isEmpty() || corpo == null || cabecalho == null
                || !cabecalho.startsWith(PREFIXO)) {
            return false;
        }
        byte[] recebida;
        try {
            recebida = HexFormat.of().parseHex(cabecalho.substring(PREFIXO.length()).strip().toLowerCase());
        } catch (IllegalArgumentException e) {
            return false;
        }
        return MessageDigest.isEqual(calcular(corpo), recebida);
    }

    /** Valida o token do desafio de verificação (GET) em tempo constante. */
    public boolean tokenVerificacaoValido(String token) {
        if (configuracao.tokenVerificacao().isEmpty() || token == null) {
            return false;
        }
        return MessageDigest.isEqual(configuracao.tokenVerificacao().getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }

    byte[] calcular(byte[] corpo) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(configuracao.segredoApp().getBytes(StandardCharsets.UTF_8), ALGORITMO));
            return mac.doFinal(corpo);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 indisponível.", e);
        }
    }
}
