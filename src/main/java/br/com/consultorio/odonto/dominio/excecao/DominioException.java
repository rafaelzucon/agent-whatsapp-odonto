package br.com.consultorio.odonto.dominio.excecao;

/**
 * Base das exceções de regra de negócio. Exceções de domínio substituem códigos de erro.
 */
public abstract class DominioException extends RuntimeException {

    protected DominioException(String mensagem) {
        super(mensagem);
    }
}
