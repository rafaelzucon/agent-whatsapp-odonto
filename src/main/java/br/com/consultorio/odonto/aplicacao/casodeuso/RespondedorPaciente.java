package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.dominio.modelo.Conversa;
import br.com.consultorio.odonto.dominio.modelo.RespostaAgente;

/**
 * Entrega ao paciente uma resposta decidida pela conversa. O {@link ConduzirConversaService} depende desta
 * abstração, não do canal usado para responder (DIP).
 */
@FunctionalInterface
public interface RespondedorPaciente {

    void responder(Conversa conversa, RespostaAgente resposta);
}
