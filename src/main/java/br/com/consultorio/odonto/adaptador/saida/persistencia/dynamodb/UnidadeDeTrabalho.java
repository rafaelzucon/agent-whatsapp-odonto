package br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Escritas acumuladas durante uma transação e enviadas juntas no commit ({@code TransactWriteItems}): ou tudo é
 * gravado, ou nada é. Duas escritas do mesmo item viram uma só (vale a última), mantendo a condição da primeira,
 * que é a que foi montada contra o estado realmente gravado na tabela.
 */
final class UnidadeDeTrabalho {

    /** Limite de itens por chamada de TransactWriteItems. */
    static final int MAXIMO_ESCRITAS = 100;

    private final Map<Chave, Escrita> escritas = new LinkedHashMap<>();
    private boolean somenteDesfazer;

    void registrar(Escrita escrita) {
        escritas.merge(escrita.chave(), escrita, (anterior, nova) -> nova.comCondicao(anterior.condicao()));
        if (escritas.size() > MAXIMO_ESCRITAS) {
            throw new IllegalStateException("Transação com mais de " + MAXIMO_ESCRITAS + " itens no DynamoDB.");
        }
    }

    /** Escrita pendente do item (para que as leituras dentro da transação enxerguem o que ela já gravou). */
    Optional<Escrita> pendente(Chave chave) {
        return Optional.ofNullable(escritas.get(chave));
    }

    List<Escrita> escritas() {
        return new ArrayList<>(escritas.values());
    }

    void descartar() {
        escritas.clear();
    }

    void marcarSomenteDesfazer() {
        somenteDesfazer = true;
    }

    boolean somenteDesfazer() {
        return somenteDesfazer;
    }
}
