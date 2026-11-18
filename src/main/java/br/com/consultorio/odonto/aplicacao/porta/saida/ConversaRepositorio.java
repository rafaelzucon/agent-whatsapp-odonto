package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.dominio.modelo.Conversa;
import br.com.consultorio.odonto.dominio.modelo.WaId;

import java.util.Optional;

/**
 * Persistência da {@link Conversa} (uma por número). Os dados coletados são cifrados. A remoção de conversas
 * inativas fica em {@link RetencaoConversas}, usada só pelas rotinas de manutenção (ISP).
 */
public interface ConversaRepositorio {

    Optional<Conversa> buscarPorWaId(WaId waId);

    Conversa salvar(Conversa conversa);
}
