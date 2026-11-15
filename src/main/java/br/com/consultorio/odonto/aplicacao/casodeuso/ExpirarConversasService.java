package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoConversa;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ExpirarConversasUseCase;
import br.com.consultorio.odonto.aplicacao.porta.saida.ConversaRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import br.com.consultorio.odonto.dominio.modelo.EtapaConversa;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Remove conversas em andamento paradas há mais tempo que a expiração configurada, descartando os dados
 * parciais coletados (minimização da LGPD). A próxima mensagem do número recomeça pelas boas-vindas.
 */
public final class ExpirarConversasService implements ExpirarConversasUseCase {

    static final Set<EtapaConversa> EM_ANDAMENTO = EnumSet.complementOf(EnumSet.of(EtapaConversa.CONCLUIDA,
            EtapaConversa.ENCERRADA_SEM_CONSENTIMENTO, EtapaConversa.ENCAMINHADA_ATENDIMENTO_HUMANO));

    private final ConversaRepositorio conversas;
    private final ConfiguracaoConversa configuracao;
    private final Relogio relogio;

    public ExpirarConversasService(ConversaRepositorio conversas, ConfiguracaoConversa configuracao, Relogio relogio) {
        this.conversas = Objects.requireNonNull(conversas);
        this.configuracao = Objects.requireNonNull(configuracao);
        this.relogio = Objects.requireNonNull(relogio);
    }

    @Override
    public int expirar() {
        return conversas.removerInativasDesde(relogio.agora().minus(configuracao.expiracao()), EM_ANDAMENTO);
    }
}
