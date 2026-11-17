package br.com.consultorio.odonto.adaptador.entrada.web;

/**
 * Proteção da API administrativa ({@code /api/admin/**}).
 *
 * @param token token exigido no cabeçalho {@code Authorization: Bearer ...}; vazio desativa a API administrativa
 */
public record ConfiguracaoAdmin(String token) {

    public ConfiguracaoAdmin {
        token = token == null ? "" : token.strip();
    }

    public boolean habilitada() {
        return !token.isEmpty();
    }

    @Override
    public String toString() {
        return "ConfiguracaoAdmin[token=***]";
    }
}
