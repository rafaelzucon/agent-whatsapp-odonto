package br.com.consultorio.odonto.dominio.servico;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normaliza textos para comparação: sem acentos, minúsculas, só letras e dígitos separados por um espaço.
 */
public final class NormalizadorTexto {

    private static final Pattern MARCAS_DIACRITICAS = Pattern.compile("\\p{M}+");
    private static final Pattern NAO_ALFANUMERICO = Pattern.compile("[^a-z0-9]+");

    private NormalizadorTexto() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = MARCAS_DIACRITICAS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
        return NAO_ALFANUMERICO.matcher(semAcento.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }

    /** Diz se o texto (já normalizado) contém a expressão como palavras inteiras. */
    public static boolean contemExpressao(String textoNormalizado, String expressao) {
        String alvo = normalizar(expressao);
        return !alvo.isEmpty() && (" " + textoNormalizado + " ").contains(" " + alvo + " ");
    }
}
