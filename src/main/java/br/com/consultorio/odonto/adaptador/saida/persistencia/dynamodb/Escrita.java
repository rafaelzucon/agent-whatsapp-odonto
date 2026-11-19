package br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

import java.util.Map;
import java.util.Optional;

/**
 * Uma gravação ({@code item} presente) ou remoção ({@code item} ausente) pendente, com condição opcional.
 *
 * @param condicao pode ser {@code null}
 */
record Escrita(Chave chave, Map<String, AttributeValue> item, Condicao condicao) {

    static Escrita gravacao(Map<String, AttributeValue> item, Condicao condicao) {
        return new Escrita(Chave.de(item), Map.copyOf(item), condicao);
    }

    static Escrita remocao(Chave chave, Condicao condicao) {
        return new Escrita(chave, null, condicao);
    }

    Optional<Map<String, AttributeValue>> itemGravado() {
        return Optional.ofNullable(item);
    }

    /** A mesma escrita com outra condição (usada ao juntar duas escritas do mesmo item na unidade de trabalho). */
    Escrita comCondicao(Condicao outra) {
        return new Escrita(chave, item, outra);
    }

    TransactWriteItem paraTransacao(String tabela) {
        if (item != null) {
            Put.Builder put = Put.builder().tableName(tabela).item(item);
            if (condicao != null) {
                put.conditionExpression(condicao.expressao());
                if (!condicao.valores().isEmpty()) {
                    put.expressionAttributeValues(condicao.valores());
                }
            }
            return TransactWriteItem.builder().put(put.build()).build();
        }
        Delete.Builder delete = Delete.builder().tableName(tabela).key(chave.mapa());
        if (condicao != null) {
            delete.conditionExpression(condicao.expressao());
            if (!condicao.valores().isEmpty()) {
                delete.expressionAttributeValues(condicao.valores());
            }
        }
        return TransactWriteItem.builder().delete(delete.build()).build();
    }
}
