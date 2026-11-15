package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Número do RG normalizado (sem pontos, traços e espaços) e a UF do órgão emissor.
 * Aceita somente dígitos, com X opcional no final, entre 5 e 14 caracteres.
 */
public record DocumentoRg(String numero, Uf uf) {

    private static final Pattern FORMATO = Pattern.compile("\\d{4,13}[\\dX]");

    public DocumentoRg {
        Objects.requireNonNull(uf, "uf");
        numero = normalizar(numero);
        if (!FORMATO.matcher(numero).matches()) {
            throw new DadoInvalidoException("RG inválido: use de 5 a 14 dígitos (o último pode ser X).");
        }
    }

    public static DocumentoRg de(String numero, String uf) {
        return new DocumentoRg(numero, Uf.de(uf));
    }

    private static String normalizar(String numero) {
        if (numero == null) {
            throw new DadoInvalidoException("RG não informado.");
        }
        return numero.replaceAll("[.\\-\\s/]", "").toUpperCase(Locale.ROOT);
    }

    /** Valor canônico usado para gerar o hash de busca (número + UF). */
    public String chaveCanonica() {
        return numero + "|" + uf.name();
    }

    /** Representação sem expor o número completo, para telas e avisos. */
    public String mascarado() {
        String fim = numero.substring(numero.length() - 2);
        return "*".repeat(numero.length() - 2) + fim + " " + uf.name();
    }

    @Override
    public String toString() {
        return mascarado();
    }
}
