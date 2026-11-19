package br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;

/**
 * Gerenciador de transações do Spring para o DynamoDB. Cada transação abre uma {@link UnidadeDeTrabalho}; o commit
 * envia as escritas acumuladas de uma vez ({@code TransactWriteItems}) e o rollback simplesmente as descarta.
 *
 * <p>Por ser um {@code PlatformTransactionManager} comum, o {@code SpringExecutorTransacional} e os ouvintes
 * {@code @TransactionalEventListener(AFTER_COMMIT)} funcionam sem nenhuma mudança (LSP: o DynamoDB entra no lugar
 * do JPA sem que a aplicação perceba).
 */
public class GerenciadorTransacaoDynamoDb extends AbstractPlatformTransactionManager {

    private final transient TabelaDynamoDb tabela;

    public GerenciadorTransacaoDynamoDb(TabelaDynamoDb tabela) {
        this.tabela = Objects.requireNonNull(tabela);
    }

    @Override
    protected Object doGetTransaction() {
        return new Transacao((UnidadeDeTrabalho) TransactionSynchronizationManager.getResource(tabela));
    }

    @Override
    protected boolean isExistingTransaction(Object transacao) {
        return ((Transacao) transacao).unidade != null;
    }

    @Override
    protected void doBegin(Object transacao, TransactionDefinition definicao) {
        UnidadeDeTrabalho unidade = new UnidadeDeTrabalho();
        ((Transacao) transacao).unidade = unidade;
        TransactionSynchronizationManager.bindResource(tabela, unidade);
    }

    @Override
    protected Object doSuspend(Object transacao) {
        ((Transacao) transacao).unidade = null;
        return TransactionSynchronizationManager.unbindResource(tabela);
    }

    @Override
    protected void doResume(Object transacao, Object suspensa) {
        TransactionSynchronizationManager.bindResource(tabela, suspensa);
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        tabela.confirmar(((Transacao) status.getTransaction()).unidade);
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        ((Transacao) status.getTransaction()).unidade.descartar();
    }

    @Override
    protected void doSetRollbackOnly(DefaultTransactionStatus status) {
        ((Transacao) status.getTransaction()).unidade.marcarSomenteDesfazer();
    }

    @Override
    protected void doCleanupAfterCompletion(Object transacao) {
        TransactionSynchronizationManager.unbindResourceIfPossible(tabela);
        ((Transacao) transacao).unidade = null;
    }

    private static final class Transacao implements SmartTransactionObject {

        private UnidadeDeTrabalho unidade;

        private Transacao(UnidadeDeTrabalho unidade) {
            this.unidade = unidade;
        }

        @Override
        public boolean isRollbackOnly() {
            return unidade != null && unidade.somenteDesfazer();
        }

        @Override
        public void flush() {
            // As escritas só vão para o DynamoDB no commit.
        }
    }
}
