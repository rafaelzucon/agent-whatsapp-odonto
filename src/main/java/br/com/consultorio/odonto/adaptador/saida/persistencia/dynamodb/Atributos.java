package br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Conversões entre os atributos do DynamoDB e tipos Java (instantes em ISO-8601, números como texto decimal). */
final class Atributos {

    private Atributos() {
    }

    /** Instante como chave de ordenação: milissegundos com zeros à esquerda (a ordem do texto = ordem do tempo). */
    static String ordenavel(Instant instante) {
        return String.format("%015d", instante.toEpochMilli());
    }

    static String ordenavel(long id) {
        return String.format("%019d", id);
    }

    static String texto(Map<String, AttributeValue> item, String nome) {
        AttributeValue valor = item.get(nome);
        return valor == null || Boolean.TRUE.equals(valor.nul()) ? null : valor.s();
    }

    static Long numero(Map<String, AttributeValue> item, String nome) {
        AttributeValue valor = item.get(nome);
        return valor == null || valor.n() == null ? null : Long.valueOf(valor.n());
    }

    static double decimal(Map<String, AttributeValue> item, String nome) {
        return Double.parseDouble(item.get(nome).n());
    }

    static boolean logico(Map<String, AttributeValue> item, String nome) {
        AttributeValue valor = item.get(nome);
        return valor != null && Boolean.TRUE.equals(valor.bool());
    }

    static Instant instante(Map<String, AttributeValue> item, String nome) {
        return Optional.ofNullable(texto(item, nome)).map(Instant::parse).orElse(null);
    }

    static List<String> textos(Map<String, AttributeValue> item, String nome) {
        AttributeValue valor = item.get(nome);
        return valor == null || !valor.hasL() ? List.of() : valor.l().stream().map(AttributeValue::s).toList();
    }

    static List<Map<String, AttributeValue>> mapas(Map<String, AttributeValue> item, String nome) {
        AttributeValue valor = item.get(nome);
        return valor == null || !valor.hasL() ? List.of() : valor.l().stream().map(AttributeValue::m).toList();
    }

    /** Montador de item que ignora valores nulos (o DynamoDB não grava atributos vazios). */
    static final class Item {

        private final Map<String, AttributeValue> atributos = new HashMap<>();

        Item com(String nome, String valor) {
            if (valor != null) {
                atributos.put(nome, AttributeValue.fromS(valor));
            }
            return this;
        }

        Item com(String nome, Long valor) {
            if (valor != null) {
                atributos.put(nome, AttributeValue.fromN(Long.toString(valor)));
            }
            return this;
        }

        Item com(String nome, long valor) {
            atributos.put(nome, AttributeValue.fromN(Long.toString(valor)));
            return this;
        }

        Item com(String nome, double valor) {
            atributos.put(nome, AttributeValue.fromN(Double.toString(valor)));
            return this;
        }

        Item com(String nome, boolean valor) {
            atributos.put(nome, AttributeValue.fromBool(valor));
            return this;
        }

        Item com(String nome, Instant valor) {
            return com(nome, valor == null ? null : valor.toString());
        }

        Item com(String nome, Enum<?> valor) {
            return com(nome, valor == null ? null : valor.name());
        }

        Item comTextos(String nome, List<String> valores) {
            atributos.put(nome, AttributeValue.fromL(valores.stream().map(AttributeValue::fromS).toList()));
            return this;
        }

        Item comMapas(String nome, List<Map<String, AttributeValue>> valores) {
            atributos.put(nome, AttributeValue.fromL(valores.stream().map(AttributeValue::fromM).toList()));
            return this;
        }

        Item chave(Chave chave) {
            return com(TabelaDynamoDb.PK, chave.pk()).com(TabelaDynamoDb.SK, chave.sk());
        }

        Item indice(TabelaDynamoDb.Indice indice, String particao, String ordenacao) {
            return com(indice.particao(), particao).com(indice.ordenacao(), ordenacao);
        }

        Map<String, AttributeValue> mapa() {
            return Map.copyOf(atributos);
        }
    }
}
