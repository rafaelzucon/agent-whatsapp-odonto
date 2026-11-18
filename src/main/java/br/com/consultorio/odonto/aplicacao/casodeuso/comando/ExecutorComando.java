package br.com.consultorio.odonto.aplicacao.casodeuso.comando;

import br.com.consultorio.odonto.dominio.modelo.AcaoConversa;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa;
import br.com.consultorio.odonto.dominio.modelo.Conversa;

/**
 * Executa um único tipo de {@link ComandoConversa} (SRP), delegando ao caso de uso correspondente e devolvendo o
 * resultado à conversa. Um comando novo ganha um executor novo, registrado na composição (OCP).
 *
 * @param <C> tipo de comando tratado
 */
public interface ExecutorComando<C extends ComandoConversa> {

    Class<C> tipo();

    AcaoConversa executar(Conversa conversa, C comando);
}
