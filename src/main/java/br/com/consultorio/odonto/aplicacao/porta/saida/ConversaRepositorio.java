package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.dominio.modelo.Conversa;
import br.com.consultorio.odonto.dominio.modelo.EtapaConversa;
import br.com.consultorio.odonto.dominio.modelo.WaId;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/** Persistência da {@link Conversa} (uma por número). Os dados coletados são cifrados. */
public interface ConversaRepositorio {

    Optional<Conversa> buscarPorWaId(WaId waId);

    Conversa salvar(Conversa conversa);

    /** Remove conversas nas etapas informadas sem interação desde {@code limite}; devolve quantas. */
    int removerInativasDesde(Instant limite, Set<EtapaConversa> etapas);
}
