package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;

import java.util.regex.Pattern;

/**
 * Identificador do WhatsApp (número internacional só com dígitos, 10 a 15 caracteres).
 */
public record WaId(String valor) {

    private static final Pattern FORMATO = Pattern.compile("\\d{10,15}");

    public WaId {
        if (valor == null) {
            throw new DadoInvalidoException("Número de WhatsApp não informado.");
        }
        valor = valor.replaceAll("\\D", "");
        if (!FORMATO.matcher(valor).matches()) {
            throw new DadoInvalidoException("Número de WhatsApp inválido.");
        }
    }

    /** Somente os 4 últimos dígitos, o máximo permitido em logs. */
    public String finalMascarado() {
        return "***" + valor.substring(valor.length() - 4);
    }

    @Override
    public String toString() {
        return finalMascarado();
    }
}
