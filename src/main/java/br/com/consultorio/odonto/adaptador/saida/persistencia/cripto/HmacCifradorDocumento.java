package br.com.consultorio.odonto.adaptador.saida.persistencia.cripto;

import br.com.consultorio.odonto.aplicacao.porta.saida.CifradorDocumento;
import br.com.consultorio.odonto.dominio.modelo.DocumentoRg;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * Gera o identificador de busca do RG com HMAC-SHA256 (determinístico, mas irreversível sem a chave).
 */
@Component
public class HmacCifradorDocumento implements CifradorDocumento {

    private static final String ALGORITMO = "HmacSHA256";

    private final SecretKeySpec chave;

    public HmacCifradorDocumento(ConfiguracaoCriptografia configuracao) {
        this.chave = new SecretKeySpec(configuracao.bytesChaveHash(), ALGORITMO);
    }

    @Override
    public String hash(DocumentoRg rg) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(chave);
            return HexFormat.of().formatHex(mac.doFinal(rg.chaveCanonica().getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao calcular o hash do RG.", e);
        }
    }
}
