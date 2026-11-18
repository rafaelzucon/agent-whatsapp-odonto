package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.casodeuso.comando.ProcessadorComandosConversa;
import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoConversa;
import br.com.consultorio.odonto.aplicacao.excecao.RecursoNaoEncontradoException;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ConduzirConversaUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.NotificarConsultorioUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.TranscreverAudioUseCase;
import br.com.consultorio.odonto.aplicacao.porta.saida.ConversaRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.MensagemRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import br.com.consultorio.odonto.dominio.modelo.AcaoConversa;
import br.com.consultorio.odonto.dominio.modelo.Conversa;
import br.com.consultorio.odonto.dominio.modelo.DirecaoMensagem;
import br.com.consultorio.odonto.dominio.modelo.Mensagem;
import br.com.consultorio.odonto.dominio.modelo.RegrasConversa;
import br.com.consultorio.odonto.dominio.modelo.TipoConteudo;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Workflow agêntico com etapas fixas: entrega a mensagem do paciente à máquina de estados da {@link Conversa},
 * executa os comandos que ela pedir (identificar, cadastrar, classificar, agendar), envia as respostas e os
 * alertas e grava o novo estado. Deve ser chamado em série para um mesmo número (o adaptador garante isso).
 */
public final class ConduzirConversaService implements ConduzirConversaUseCase {

    /** Limite de comandos encadeados por mensagem; protege contra laços (ex.: corridas repetidas na agenda). */
    static final int MAXIMO_PASSOS = 10;

    private static final System.Logger LOG = System.getLogger(ConduzirConversaService.class.getName());

    private final MensagemRepositorio mensagens;
    private final ConversaRepositorio conversas;
    private final TranscreverAudioUseCase transcreverAudio;
    private final NotificarConsultorioUseCase notificarConsultorio;
    private final ProcessadorComandosConversa processador;
    private final RespondedorPaciente respondedor;
    private final ConfiguracaoConversa configuracao;
    private final Relogio relogio;

    public ConduzirConversaService(MensagemRepositorio mensagens, ConversaRepositorio conversas,
                                   TranscreverAudioUseCase transcreverAudio,
                                   NotificarConsultorioUseCase notificarConsultorio,
                                   ProcessadorComandosConversa processador, RespondedorPaciente respondedor,
                                   ConfiguracaoConversa configuracao, Relogio relogio) {
        this.mensagens = Objects.requireNonNull(mensagens);
        this.conversas = Objects.requireNonNull(conversas);
        this.transcreverAudio = Objects.requireNonNull(transcreverAudio);
        this.notificarConsultorio = Objects.requireNonNull(notificarConsultorio);
        this.processador = Objects.requireNonNull(processador);
        this.respondedor = Objects.requireNonNull(respondedor);
        this.configuracao = Objects.requireNonNull(configuracao);
        this.relogio = Objects.requireNonNull(relogio);
    }

    @Override
    public void conduzir(long mensagemId) {
        Mensagem mensagem = mensagens.buscarPorId(mensagemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Mensagem " + mensagemId + " não encontrada."));
        if (mensagem.direcao() != DirecaoMensagem.RECEBIDA) {
            return;
        }
        Instant agora = relogio.agora();
        RegrasConversa regras = configuracao.regras();
        Conversa conversa = conversas.buscarPorWaId(mensagem.waId())
                .orElseGet(() -> Conversa.nova(mensagem.waId(), agora));

        Optional<String> texto = textoDa(mensagem);
        AcaoConversa acao = texto
                .map(t -> conversa.registrarResposta(t, agora, relogio.hoje(), regras))
                .orElseGet(() -> conversa.conteudoNaoTextual(agora, relogio.hoje(), regras));

        for (int passo = 1; ; passo++) {
            acao.respostas().forEach(resposta -> respondedor.responder(conversa, resposta));
            acao.alerta().ifPresent(notificarConsultorio::alertar);
            if (acao.comando().isEmpty()) {
                break;
            }
            if (passo >= MAXIMO_PASSOS) {
                LOG.log(Level.ERROR, "Conversa do WhatsApp final {0} excedeu {1} passos; comando {2} descartado.",
                        conversa.waId().finalMascarado(), MAXIMO_PASSOS, acao.comando().get());
                break;
            }
            acao = processador.executar(conversa, acao.comando().get());
        }
        conversas.salvar(conversa);
    }

    /** Texto da mensagem; áudios são transcritos (se houver transcritor) e a transcrição fica no histórico. */
    private Optional<String> textoDa(Mensagem mensagem) {
        Optional<String> texto = mensagem.textoParaConversa();
        if (texto.isPresent() || mensagem.tipo() != TipoConteudo.AUDIO || mensagem.midiaId().isEmpty()) {
            return texto;
        }
        Optional<String> transcricao = transcreverAudio.transcrever(mensagem.midiaId().get());
        transcricao.ifPresent(t -> {
            mensagem.registrarTranscricao(t);
            mensagens.salvar(mensagem);
        });
        return transcricao;
    }
}
