package br.com.consultorio.odonto.adaptador.entrada.lambda;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Requisição HTTP mínima, independente do formato de evento do provedor (Function URL, API Gateway...). */
public record RequisicaoHttp(String metodo, Map<String, String> parametros, Map<String, String> cabecalhos,
                             byte[] corpo) {

    public RequisicaoHttp {
        metodo = metodo == null ? "" : metodo.toUpperCase(Locale.ROOT);
        parametros = parametros == null ? Map.of() : Map.copyOf(parametros);
        TreeMap<String, String> semCaixa = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (cabecalhos != null) {
            semCaixa.putAll(cabecalhos);
        }
        cabecalhos = semCaixa;
        corpo = corpo == null ? new byte[0] : corpo;
    }

    public Optional<String> parametro(String nome) {
        return Optional.ofNullable(parametros.get(nome));
    }

    /** Cabeçalhos HTTP não diferenciam maiúsculas de minúsculas. */
    public Optional<String> cabecalho(String nome) {
        return Optional.ofNullable(cabecalhos.get(nome));
    }
}
