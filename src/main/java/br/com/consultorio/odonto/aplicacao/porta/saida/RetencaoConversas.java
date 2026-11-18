package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.dominio.modelo.EtapaConversa;

import java.time.Instant;
import java.util.Set;

/** Remoção de conversas inativas (expiração de conversas paradas e retenção das encerradas). */
public interface RetencaoConversas {

    /** Remove conversas nas etapas informadas sem interação desde {@code limite}; devolve quantas. */
    int removerInativasDesde(Instant limite, Set<EtapaConversa> etapas);
}
