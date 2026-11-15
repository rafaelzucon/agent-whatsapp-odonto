package br.com.consultorio.odonto.aplicacao.excecao;

/** Recurso pedido (paciente, consulta, queixa, bloqueio) não existe. Vira HTTP 404 na API. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
