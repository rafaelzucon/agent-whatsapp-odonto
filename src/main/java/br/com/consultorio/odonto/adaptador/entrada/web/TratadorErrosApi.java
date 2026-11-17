package br.com.consultorio.odonto.adaptador.entrada.web;

import br.com.consultorio.odonto.aplicacao.excecao.RecursoNaoEncontradoException;
import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;
import br.com.consultorio.odonto.dominio.excecao.HorarioIndisponivelException;
import br.com.consultorio.odonto.dominio.excecao.TransicaoInvalidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Traduz as exceções de domínio e de aplicação em respostas RFC 9457 (ProblemDetail). */
@RestControllerAdvice
public class TratadorErrosApi extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DadoInvalidoException.class)
    ProblemDetail dadoInvalido(DadoInvalidoException e) {
        return problema(HttpStatus.BAD_REQUEST, "Dado inválido", e.getMessage());
    }

    @ExceptionHandler(HorarioIndisponivelException.class)
    ProblemDetail horarioIndisponivel(HorarioIndisponivelException e) {
        ProblemDetail problema = problema(HttpStatus.CONFLICT, "Horário indisponível", e.getMessage());
        problema.setProperty("motivo", e.motivo().name());
        return problema;
    }

    @ExceptionHandler(TransicaoInvalidaException.class)
    ProblemDetail transicaoInvalida(TransicaoInvalidaException e) {
        return problema(HttpStatus.CONFLICT, "Operação não permitida no estado atual", e.getMessage());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Não encontrado", e.getMessage());
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
}
