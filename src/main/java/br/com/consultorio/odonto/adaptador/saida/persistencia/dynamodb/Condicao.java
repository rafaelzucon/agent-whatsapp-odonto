package br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Expressão de condição de uma escrita e a exceção (do domínio ou da aplicação) que ela representa quando falha.
 * Faz no DynamoDB o papel das restrições únicas do PostgreSQL.
 */
public record Condicao(String expressao, Map<String, AttributeValue> valores,
                       Supplier<? extends RuntimeException> violacao) {

    public Condicao {
        Objects.requireNonNull(expressao, "expressao");
        valores = Map.copyOf(valores);
        Objects.requireNonNull(violacao, "violacao");
    }

    public static Condicao de(String expressao, Supplier<? extends RuntimeException> violacao) {
        return new Condicao(expressao, Map.of(), violacao);
    }

    public static Condicao de(String expressao, Map<String, AttributeValue> valores,
                              Supplier<? extends RuntimeException> violacao) {
        return new Condicao(expressao, valores, violacao);
    }
}
