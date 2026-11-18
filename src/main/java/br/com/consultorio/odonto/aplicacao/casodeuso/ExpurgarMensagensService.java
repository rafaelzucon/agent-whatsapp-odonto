package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoRetencao;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ExpurgarMensagensUseCase;
import br.com.consultorio.odonto.aplicacao.porta.saida.ExecutorTransacional;
import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import br.com.consultorio.odonto.aplicacao.porta.saida.RetencaoConversas;
import br.com.consultorio.odonto.aplicacao.porta.saida.RetencaoMensagens;
import br.com.consultorio.odonto.dominio.modelo.EtapaConversa;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Aplica a retenção da LGPD ao histórico do WhatsApp: apaga mensagens antigas e conversas encerradas.
 * Pacientes, prontuários, queixas e consultas NÃO são apagados (prontuário tem guarda legal mínima).
 */
public final class ExpurgarMensagensService implements ExpurgarMensagensUseCase {

    static final Set<EtapaConversa> ENCERRADAS = EnumSet.of(EtapaConversa.CONCLUIDA,
            EtapaConversa.ENCERRADA_SEM_CONSENTIMENTO, EtapaConversa.ENCAMINHADA_ATENDIMENTO_HUMANO);

    private final RetencaoMensagens mensagens;
    private final RetencaoConversas conversas;
    private final ConfiguracaoRetencao retencao;
    private final ExecutorTransacional transacao;
    private final Relogio relogio;

    public ExpurgarMensagensService(RetencaoMensagens mensagens, RetencaoConversas conversas,
                                    ConfiguracaoRetencao retencao, ExecutorTransacional transacao, Relogio relogio) {
        this.mensagens = Objects.requireNonNull(mensagens);
        this.conversas = Objects.requireNonNull(conversas);
        this.retencao = Objects.requireNonNull(retencao);
        this.transacao = Objects.requireNonNull(transacao);
        this.relogio = Objects.requireNonNull(relogio);
    }

    @Override
    public ResultadoExpurgo expurgar() {
        Instant agora = relogio.agora();
        return transacao.executar(() -> new ResultadoExpurgo(
                mensagens.removerAnterioresA(agora.minus(Duration.ofDays(retencao.mensagensDias()))),
                conversas.removerInativasDesde(agora.minus(Duration.ofDays(retencao.conversasEncerradasDias())),
                        ENCERRADAS)));
    }
}
