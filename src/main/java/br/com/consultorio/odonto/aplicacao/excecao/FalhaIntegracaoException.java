package br.com.consultorio.odonto.aplicacao.excecao;

/**
 * Falha ao falar com um sistema externo (WhatsApp, LLM, transcrição, e-mail). Os adaptadores de saída
 * traduzem erros de rede e respostas inesperadas para esta exceção, para que os casos de uso decidam
 * o que fazer (fallback, nova tentativa ou registro em log) sem conhecer a tecnologia usada.
 */
public class FalhaIntegracaoException extends RuntimeException {

    public FalhaIntegracaoException(String mensagem) {
        super(mensagem);
    }

    public FalhaIntegracaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
