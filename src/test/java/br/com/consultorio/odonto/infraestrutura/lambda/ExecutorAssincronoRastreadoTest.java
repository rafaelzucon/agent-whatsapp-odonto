package br.com.consultorio.odonto.infraestrutura.lambda;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutorAssincronoRastreadoTest {

    private final ExecutorAssincronoRastreado executor = new ExecutorAssincronoRastreado();

    @Test
    void aguardaInclusiveAsTarefasDisparadasPorOutrasTarefas() throws Exception {
        AtomicInteger concluidas = new AtomicInteger();
        executor.execute(() -> {
            dormir(50);
            // Ex.: a condução da conversa agenda a consulta e dispara o aviso ao consultório.
            executor.execute(() -> {
                dormir(50);
                concluidas.incrementAndGet();
            });
            concluidas.incrementAndGet();
        });

        assertThat(executor.aguardarConclusao(Duration.ofSeconds(5))).isTrue();
        assertThat(concluidas).hasValue(2);
        assertThat(executor.emAndamento()).isZero();
    }

    @Test
    void devolveFalsoQuandoOLimiteAcabaETerminaDepoisMesmoComFalha() throws Exception {
        CountDownLatch liberar = new CountDownLatch(1);
        executor.execute(() -> {
            aguardar(liberar);
            throw new IllegalStateException("falha na tarefa");
        });

        assertThat(executor.aguardarConclusao(Duration.ofMillis(50))).isFalse();
        liberar.countDown();
        assertThat(executor.aguardarConclusao(Duration.ofSeconds(5))).isTrue();
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void aguardar(CountDownLatch trava) {
        try {
            trava.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
