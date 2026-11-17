package br.com.consultorio.odonto.adaptador.entrada.webhook;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class AssinaturaWebhookVerifierTest {

    private static final byte[] CORPO = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);

    private final AssinaturaWebhookVerifier verificador =
            new AssinaturaWebhookVerifier(new ConfiguracaoWebhook("token-desafio", "segredo-do-app", ""));

    private String assinatura(byte[] corpo) {
        return "sha256=" + HexFormat.of().formatHex(verificador.calcular(corpo));
    }

    @Test
    void aceitaAssinaturaCorretaInclusiveEmMaiusculas() {
        assertThat(verificador.assinaturaValida(CORPO, assinatura(CORPO))).isTrue();
        assertThat(verificador.assinaturaValida(CORPO, "sha256=" + assinatura(CORPO).substring(7).toUpperCase()))
                .isTrue();
    }

    @Test
    void rejeitaCorpoAlteradoCabecalhoAusenteOuMalFormado() {
        byte[] alterado = "{\"object\":\"outro\"}".getBytes(StandardCharsets.UTF_8);
        assertThat(verificador.assinaturaValida(alterado, assinatura(CORPO))).isFalse();
        assertThat(verificador.assinaturaValida(CORPO, null)).isFalse();
        assertThat(verificador.assinaturaValida(CORPO, "sha1=abc")).isFalse();
        assertThat(verificador.assinaturaValida(CORPO, "sha256=zz-nao-hex")).isFalse();
    }

    @Test
    void semSegredoConfiguradoNenhumaAssinaturaEhAceita() {
        AssinaturaWebhookVerifier semSegredo = new AssinaturaWebhookVerifier(new ConfiguracaoWebhook("t", null, null));
        assertThat(semSegredo.assinaturaValida(CORPO, assinatura(CORPO))).isFalse();
    }

    @Test
    void validaTokenDoDesafioDeVerificacao() {
        assertThat(verificador.tokenVerificacaoValido("token-desafio")).isTrue();
        assertThat(verificador.tokenVerificacaoValido("outro")).isFalse();
        assertThat(verificador.tokenVerificacaoValido(null)).isFalse();
        assertThat(new AssinaturaWebhookVerifier(new ConfiguracaoWebhook("", "s", "")).tokenVerificacaoValido(""))
                .isFalse();
    }
}
