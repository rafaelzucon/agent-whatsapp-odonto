package br.com.consultorio.odonto.infraestrutura.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/** Espera o processamento assíncrono (condução da conversa, aviso ao consultório) antes de a invocação acabar. */
final class AguardaTarefasAssincronas {

    private static final Logger LOG = LoggerFactory.getLogger(AguardaTarefasAssincronas.class);
    private static final long MARGEM_MS = 2_000;
    private static final long SEM_CONTEXTO_MS = 60_000;

    private AguardaTarefasAssincronas() {
    }

    static void aguardar(ExecutorAssincronoRastreado executor, Context contexto) {
        long restante = contexto == null ? SEM_CONTEXTO_MS : contexto.getRemainingTimeInMillis();
        Duration limite = Duration.ofMillis(Math.max(1_000, restante - MARGEM_MS));
        try {
            if (!executor.aguardarConclusao(limite)) {
                LOG.error("Tempo da invocação esgotado com {} tarefa(s) assíncrona(s) em andamento; aumente o "
                        + "timeout da função.", executor.emAndamento());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrompido aguardando as tarefas assíncronas.", e);
        }
    }
}
