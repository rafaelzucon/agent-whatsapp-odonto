package br.com.consultorio.odonto.aplicacao.porta.saida;

import java.util.function.Supplier;

/** Executa um trecho dentro de uma transação: tudo é gravado junto, ou nada é gravado. */
public interface ExecutorTransacional {

    /** Executa e devolve o resultado; qualquer exceção desfaz a transação e é repassada. */
    <T> T executar(Supplier<T> operacao);

    default void executarSemRetorno(Runnable operacao) {
        executar(() -> {
            operacao.run();
            return null;
        });
    }
}
