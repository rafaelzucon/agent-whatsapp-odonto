package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoNotificacao;
import br.com.consultorio.odonto.aplicacao.evento.MensagemRecebidaEvento;
import br.com.consultorio.odonto.aplicacao.excecao.MensagemDuplicadaException;
import br.com.consultorio.odonto.aplicacao.porta.entrada.MensagemRecebidaCommand;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ReceberMensagensUseCase;
import br.com.consultorio.odonto.aplicacao.porta.saida.ExecutorTransacional;
import br.com.consultorio.odonto.aplicacao.porta.saida.MensagemRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.PublicadorEventos;
import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;
import br.com.consultorio.odonto.dominio.modelo.Mensagem;
import br.com.consultorio.odonto.dominio.modelo.WaId;

import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Objects;

/**
 * Grava as mensagens recebidas pelo webhook (uma única vez cada) e publica um evento para que a conversa
 * seja conduzida de forma assíncrona, depois que a transação for confirmada. Mensagens repetidas pela Meta
 * (reentregas do webhook) e mensagens vindas do próprio número de notificação do consultório são ignoradas.
 */
public final class ReceberMensagensService implements ReceberMensagensUseCase {

    private static final System.Logger LOG = System.getLogger(ReceberMensagensService.class.getName());

    private final MensagemRepositorio mensagens;
    private final PublicadorEventos publicador;
    private final ConfiguracaoNotificacao configuracao;
    private final ExecutorTransacional transacao;

    public ReceberMensagensService(MensagemRepositorio mensagens, PublicadorEventos publicador,
                                   ConfiguracaoNotificacao configuracao, ExecutorTransacional transacao) {
        this.mensagens = Objects.requireNonNull(mensagens);
        this.publicador = Objects.requireNonNull(publicador);
        this.configuracao = Objects.requireNonNull(configuracao);
        this.transacao = Objects.requireNonNull(transacao);
    }

    @Override
    public ResultadoRecebimento receber(List<MensagemRecebidaCommand> comandos) {
        int novas = 0;
        int duplicadas = 0;
        int ignoradas = 0;
        for (MensagemRecebidaCommand comando : comandos) {
            switch (receber(comando)) {
                case NOVA -> novas++;
                case DUPLICADA -> duplicadas++;
                case IGNORADA -> ignoradas++;
            }
        }
        return new ResultadoRecebimento(novas, duplicadas, ignoradas);
    }

    private Situacao receber(MensagemRecebidaCommand comando) {
        WaId waId;
        try {
            waId = new WaId(comando.waId());
        } catch (DadoInvalidoException e) {
            LOG.log(Level.WARNING, "Mensagem {0} ignorada: número de WhatsApp inválido.", comando.waMessageId());
            return Situacao.IGNORADA;
        }
        if (configuracao.ehNumeroDoConsultorio(waId)) {
            return Situacao.IGNORADA;
        }
        if (mensagens.existePorWaMessageId(comando.waMessageId())) {
            return Situacao.DUPLICADA;
        }
        try {
            transacao.executarSemRetorno(() -> {
                Mensagem salva = mensagens.salvar(Mensagem.recebida(comando.waMessageId(), waId, comando.tipo(),
                        comando.texto(), comando.midiaId(), comando.recebidaEm()));
                publicador.publicar(new MensagemRecebidaEvento(salva.id().orElseThrow(), waId.valor()));
            });
            return Situacao.NOVA;
        } catch (MensagemDuplicadaException e) {
            return Situacao.DUPLICADA;
        }
    }

    private enum Situacao { NOVA, DUPLICADA, IGNORADA }
}
