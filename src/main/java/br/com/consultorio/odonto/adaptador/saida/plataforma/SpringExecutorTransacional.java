package br.com.consultorio.odonto.adaptador.saida.plataforma;

import br.com.consultorio.odonto.aplicacao.porta.saida.ExecutorTransacional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/** Executa trechos dos casos de uso em uma transação do Spring (propagação REQUIRED). */
@Component
public class SpringExecutorTransacional implements ExecutorTransacional {

    private final TransactionTemplate transacao;

    public SpringExecutorTransacional(PlatformTransactionManager gerenciador) {
        this.transacao = new TransactionTemplate(gerenciador);
    }

    @Override
    public <T> T executar(Supplier<T> operacao) {
        return transacao.execute(status -> operacao.get());
    }
}
