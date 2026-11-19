package br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Objects;

/** Chave primária de um item da tabela única (partição {@code pk} + ordenação {@code sk}). */
public record Chave(String pk, String sk) {

    public Chave {
        Objects.requireNonNull(pk, "pk");
        Objects.requireNonNull(sk, "sk");
    }

    Map<String, AttributeValue> mapa() {
        return Map.of(TabelaDynamoDb.PK, AttributeValue.fromS(pk), TabelaDynamoDb.SK, AttributeValue.fromS(sk));
    }

    static Chave de(Map<String, AttributeValue> item) {
        return new Chave(item.get(TabelaDynamoDb.PK).s(), item.get(TabelaDynamoDb.SK).s());
    }
}
