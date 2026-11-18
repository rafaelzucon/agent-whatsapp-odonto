package br.com.consultorio.odonto.aplicacao.casodeuso.comando;

import br.com.consultorio.odonto.dominio.modelo.AcaoConversa;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa;
import br.com.consultorio.odonto.dominio.modelo.Conversa;

/**
 * Executa um comando emitido pela máquina de estados da conversa e devolve a ação seguinte decidida por ela.
 * O {@code ConduzirConversaService} depende desta abstração, não de como os comandos são executados (DIP).
 */
@FunctionalInterface
public interface ProcessadorComandosConversa {

    AcaoConversa executar(Conversa conversa, ComandoConversa comando);
}
