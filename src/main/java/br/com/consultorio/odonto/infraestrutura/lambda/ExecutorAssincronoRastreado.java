package br.com.consultorio.odonto.infraestrutura.lambda;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Executor dos métodos {@code @Async} no modo Lambda. Roda cada tarefa em uma virtual thread (como no modo
 * servidor) e conta as tarefas em andamento, para o handler aguardar a conclusão antes de encerrar a
 * invocação: depois que o handler retorna, o Lambda congela o ambiente e nada mais executa.
 *
 * <p>Por que não executar os ouvintes de forma síncrona? Eles são {@code AFTER_COMMIT}: rodando na mesma
 * thread, o acesso a dados ainda participaria da transação já confirmada e as gravações seriam perdidas.
 * Em outra thread, cada caso de uso abre a própria transação, exatamente como no modo servidor.
 */
public class ExecutorAssincronoRastreado implements Executor {

    private final ReentrantLock trava = new ReentrantLock();
    private final Condition ocioso = trava.newCondition();
    private final Thread.Builder fabrica = Thread.ofVirtual().name("odonto-async-", 0);
    private int emAndamento;

    @Override
    public void execute(Runnable tarefa) {
        alterar(+1);
        try {
            fabrica.start(() -> {
                try {
                    tarefa.run();
                } finally {
                    alterar(-1);
                }
            });
        } catch (RuntimeException | Error e) {
            alterar(-1);
            throw e;
        }
    }

    /**
     * Espera todas as tarefas (inclusive as disparadas por outras tarefas) terminarem.
     *
     * @return {@code false} se o limite acabou antes
     */
    public boolean aguardarConclusao(Duration limite) throws InterruptedException {
        long restanteNanos = limite.toNanos();
        trava.lock();
        try {
            while (emAndamento > 0) {
                if (restanteNanos <= 0) {
                    return false;
                }
                restanteNanos = ocioso.awaitNanos(restanteNanos);
            }
            return true;
        } finally {
            trava.unlock();
        }
    }

    public int emAndamento() {
        trava.lock();
        try {
            return emAndamento;
        } finally {
            trava.unlock();
        }
    }

    private void alterar(int delta) {
        trava.lock();
        try {
            emAndamento += delta;
            if (emAndamento == 0) {
                ocioso.signalAll();
            }
        } finally {
            trava.unlock();
        }
    }
}
