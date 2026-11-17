package br.com.consultorio.odonto.adaptador.entrada.evento;

import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Serializa o processamento das mensagens de um mesmo número: duas mensagens seguidas do paciente nunca
 * conduzem a mesma conversa ao mesmo tempo. Usa um conjunto fixo de travas (memória limitada) e
 * {@link ReentrantLock}, que não prende a thread de plataforma das virtual threads.
 * <p>
 * Vale para uma única instância da aplicação (ver README, "Limitações").
 */
@Component
public class TravasPorConversa {

    private static final int QUANTIDADE = 256;

    private final ReentrantLock[] travas = new ReentrantLock[QUANTIDADE];

    public TravasPorConversa() {
        for (int i = 0; i < QUANTIDADE; i++) {
            travas[i] = new ReentrantLock(true);
        }
    }

    public void executar(String chave, Runnable tarefa) {
        ReentrantLock trava = travas[Math.floorMod(chave.hashCode(), QUANTIDADE)];
        trava.lock();
        try {
            tarefa.run();
        } finally {
            trava.unlock();
        }
    }
}
