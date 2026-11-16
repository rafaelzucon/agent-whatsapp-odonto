package br.com.consultorio.odonto.adaptador.saida.persistencia.cripto;

import br.com.consultorio.odonto.dominio.modelo.DocumentoRg;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class CriptografiaServiceTest {

    static final String CHAVE_A = chave(1);
    static final String CHAVE_B = chave(2);

    static String chave(int semente) {
        byte[] bytes = new byte[32];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i * 7 + semente);
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    private final CriptografiaService servico = new CriptografiaService(new ConfiguracaoCriptografia(CHAVE_A, CHAVE_B));

    @Test
    void cifraEDecifraComIvAleatorio() {
        String texto = "Maria da Silva — dor no dente 36";

        String c1 = servico.cifrar(texto);
        String c2 = servico.cifrar(texto);

        assertThat(c1).startsWith("v1:").doesNotContain("Maria").isNotEqualTo(c2);
        assertThat(servico.decifrar(c1)).isEqualTo(texto);
        assertThat(servico.decifrar(c2)).isEqualTo(texto);
        assertThat(servico.cifrar(null)).isNull();
        assertThat(servico.decifrar(null)).isNull();
    }

    @Test
    void rejeitaDadoAdulteradoOuChaveErrada() {
        String cifrado = servico.cifrar("segredo");
        char ultimo = cifrado.charAt(cifrado.length() - 3);
        String adulterado = cifrado.substring(0, cifrado.length() - 3) + (ultimo == 'A' ? 'B' : 'A')
                + cifrado.substring(cifrado.length() - 2);
        CriptografiaService outraChave = new CriptografiaService(new ConfiguracaoCriptografia(chave(9), CHAVE_B));

        assertThatIllegalStateException().isThrownBy(() -> servico.decifrar(adulterado));
        assertThatIllegalStateException().isThrownBy(() -> outraChave.decifrar(cifrado));
        assertThatIllegalStateException().isThrownBy(() -> servico.decifrar("texto em claro"));
    }

    @Test
    void validaAsChaves() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ConfiguracaoCriptografia("", CHAVE_B))
                .withMessageContaining("openssl rand -base64 32");
        assertThatIllegalArgumentException().isThrownBy(() -> new ConfiguracaoCriptografia("curta", CHAVE_B));
        assertThatIllegalArgumentException().isThrownBy(() -> new ConfiguracaoCriptografia(CHAVE_A, CHAVE_A))
                .withMessageContaining("diferentes");
        assertThat(new ConfiguracaoCriptografia(CHAVE_A, CHAVE_B).toString()).doesNotContain(CHAVE_A);
    }

    @Test
    void hashDoRgEhDeterministicoENaoExpoeONumero() {
        HmacCifradorDocumento hmac = new HmacCifradorDocumento(new ConfiguracaoCriptografia(CHAVE_A, CHAVE_B));
        HmacCifradorDocumento outraChave = new HmacCifradorDocumento(new ConfiguracaoCriptografia(CHAVE_A, chave(5)));

        String hash = hmac.hash(DocumentoRg.de("12.345.678-9", "SP"));

        assertThat(hash).hasSize(64).doesNotContain("123456789");
        assertThat(hmac.hash(DocumentoRg.de("123456789", "sp"))).isEqualTo(hash);
        assertThat(hmac.hash(DocumentoRg.de("123456789", "RJ"))).isNotEqualTo(hash);
        assertThat(outraChave.hash(DocumentoRg.de("123456789", "SP"))).isNotEqualTo(hash);
    }
}
