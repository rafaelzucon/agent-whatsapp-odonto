package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Unidades federativas do Brasil (UF do órgão emissor do RG).
 */
public enum Uf {
    AC, AL, AP, AM, BA, CE, DF, ES, GO, MA, MT, MS, MG, PA, PB, PR, PE, PI, RJ, RN, RS, RO, RR, SC, SP, SE, TO;

    public static Optional<Uf> buscar(String sigla) {
        if (sigla == null) {
            return Optional.empty();
        }
        String normalizada = sigla.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(uf -> uf.name().equals(normalizada)).findFirst();
    }

    public static Uf de(String sigla) {
        return buscar(sigla).orElseThrow(() -> new DadoInvalidoException("UF inválida: informe a sigla do estado, por exemplo SP."));
    }
}
