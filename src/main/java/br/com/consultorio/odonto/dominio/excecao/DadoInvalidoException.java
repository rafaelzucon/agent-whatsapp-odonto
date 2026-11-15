package br.com.consultorio.odonto.dominio.excecao;

/**
 * Um dado informado (pelo paciente ou pela API) não atende às regras de validação.
 */
public class DadoInvalidoException extends DominioException {

    public DadoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
