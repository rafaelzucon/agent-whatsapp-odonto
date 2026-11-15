package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.config.MensagensConversa;
import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;
import br.com.consultorio.odonto.aplicacao.porta.saida.EnviadorWhatsApp;
import br.com.consultorio.odonto.aplicacao.porta.saida.MensagemRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import br.com.consultorio.odonto.dominio.modelo.Conversa;
import br.com.consultorio.odonto.dominio.modelo.Mensagem;
import br.com.consultorio.odonto.dominio.modelo.RespostaAgente;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Renderiza os textos configurados, envia ao paciente pelo WhatsApp (só dentro da janela de 24h da Meta) e
 * registra cada mensagem enviada no histórico.
 */
public final class RespondedorPaciente {

    private static final System.Logger LOG = System.getLogger(RespondedorPaciente.class.getName());

    private final EnviadorWhatsApp enviador;
    private final MensagemRepositorio mensagens;
    private final MensagensConversa textos;
    private final Relogio relogio;

    public RespondedorPaciente(EnviadorWhatsApp enviador, MensagemRepositorio mensagens, MensagensConversa textos,
                               Relogio relogio) {
        this.enviador = Objects.requireNonNull(enviador);
        this.mensagens = Objects.requireNonNull(mensagens);
        this.textos = Objects.requireNonNull(textos);
        this.relogio = Objects.requireNonNull(relogio);
    }

    public void responder(Conversa conversa, RespostaAgente resposta) {
        Instant agora = relogio.agora();
        // Mensagem livre fora da janela de 24h é rejeitada pela Meta; a regra fica explícita no domínio.
        conversa.janelaAtendimento().exigirAberta(agora);
        String texto = textos.renderizar(resposta);
        Optional<String> wamid = Optional.empty();
        try {
            wamid = Optional.ofNullable(enviador.enviarTexto(conversa.waId(), texto));
        } catch (FalhaIntegracaoException e) {
            LOG.log(Level.ERROR, "Falha ao enviar {0} para o WhatsApp final {1}: {2}",
                    resposta.tipo(), conversa.waId().finalMascarado(), e.getMessage());
        }
        mensagens.salvar(Mensagem.enviadaPeloAgente(wamid, conversa.waId(), texto, agora));
    }
}
