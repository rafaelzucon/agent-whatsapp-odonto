package br.com.consultorio.odonto.dominio.excecao;

/**
 * Tentativa de mudar o estado de um objeto de domínio a partir de um estado que não permite a transição.
 */
public class TransicaoInvalidaException extends DominioException {

    public TransicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
