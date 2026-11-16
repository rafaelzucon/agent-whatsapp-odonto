package br.com.consultorio.odonto.adaptador.saida.persistencia.cripto;

import java.util.Arrays;
import java.util.Base64;

/**
 * Chaves de criptografia dos dados pessoais, em Base64 (32 bytes cada).
 *
 * @param chaveDados chave AES-256 usada para cifrar os dados pessoais em repouso
 * @param chaveHash  chave HMAC-SHA256 usada para indexar o RG sem gravá-lo em claro
 */
public record ConfiguracaoCriptografia(String chaveDados, String chaveHash) {

    static final int TAMANHO_CHAVE = 32;

    public ConfiguracaoCriptografia {
        decodificar(chaveDados, "chave de dados");
        decodificar(chaveHash, "chave de hash");
        if (Arrays.equals(decodificar(chaveDados, "chave de dados"), decodificar(chaveHash, "chave de hash"))) {
            throw new IllegalArgumentException("A chave de dados e a chave de hash devem ser diferentes.");
        }
    }

    byte[] bytesChaveDados() {
        return decodificar(chaveDados, "chave de dados");
    }

    byte[] bytesChaveHash() {
        return decodificar(chaveHash, "chave de hash");
    }

    private static byte[] decodificar(String chave, String nome) {
        if (chave == null || chave.isBlank()) {
            throw new IllegalArgumentException("A " + nome + " não foi configurada. Gere com: openssl rand -base64 32");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(chave.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("A " + nome + " não está em Base64 válido.", e);
        }
        if (bytes.length != TAMANHO_CHAVE) {
            throw new IllegalArgumentException("A " + nome + " deve ter 32 bytes (256 bits) em Base64.");
        }
        return bytes;
    }

    @Override
    public String toString() {
        return "ConfiguracaoCriptografia[chaves ocultas]";
    }
}
