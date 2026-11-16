package br.com.consultorio.odonto.adaptador.saida.persistencia.cripto;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra e decifra textos com AES-256-GCM (confidencialidade + integridade).
 * Formato gravado: {@code v1:} + Base64(IV de 12 bytes + texto cifrado + tag de 16 bytes).
 */
@Component
public class CriptografiaService {

    private static final String PREFIXO = "v1:";
    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int TAMANHO_IV = 12;
    private static final int TAMANHO_TAG_BITS = 128;

    private final SecretKey chave;
    private final SecureRandom aleatorio = new SecureRandom();

    public CriptografiaService(ConfiguracaoCriptografia configuracao) {
        this.chave = new SecretKeySpec(configuracao.bytesChaveDados(), "AES");
    }

    public String cifrar(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            byte[] iv = new byte[TAMANHO_IV];
            aleatorio.nextBytes(iv);
            Cipher cifra = Cipher.getInstance(ALGORITMO);
            cifra.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(TAMANHO_TAG_BITS, iv));
            byte[] cifrado = cifra.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return PREFIXO + Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar dado pessoal.", e);
        }
    }

    public String decifrar(String valor) {
        if (valor == null) {
            return null;
        }
        if (!valor.startsWith(PREFIXO)) {
            throw new IllegalStateException("Dado cifrado em formato desconhecido.");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(valor.substring(PREFIXO.length()));
            Cipher cifra = Cipher.getInstance(ALGORITMO);
            cifra.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(TAMANHO_TAG_BITS, bytes, 0, TAMANHO_IV));
            return new String(cifra.doFinal(bytes, TAMANHO_IV, bytes.length - TAMANHO_IV), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Falha ao decifrar dado pessoal (chave errada ou dado adulterado).", e);
        }
    }
}
