package br.com.consultorio.odonto.adaptador.saida.persistencia;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.Locale;

/** Identifica qual restrição do banco causou uma {@link DataIntegrityViolationException}. */
final class ViolacaoRestricao {

    private ViolacaoRestricao() {
    }

    static boolean envolve(DataIntegrityViolationException e, String nomeRestricao) {
        Throwable causa = e.getMostSpecificCause();
        String mensagem = causa == null ? e.getMessage() : causa.getMessage();
        return mensagem != null && mensagem.toLowerCase(Locale.ROOT).contains(nomeRestricao.toLowerCase(Locale.ROOT));
    }
}
