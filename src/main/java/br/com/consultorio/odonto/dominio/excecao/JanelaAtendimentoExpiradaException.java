package br.com.consultorio.odonto.dominio.excecao;

/**
 * A janela de 24 horas da Meta para respostas livres está fechada; só templates aprovados podem ser enviados.
 */
public class JanelaAtendimentoExpiradaException extends DominioException {

    public JanelaAtendimentoExpiradaException(String mensagem) {
        super(mensagem);
    }
}
