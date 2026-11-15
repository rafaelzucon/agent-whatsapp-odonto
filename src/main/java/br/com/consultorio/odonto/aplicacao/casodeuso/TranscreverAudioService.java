package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;
import br.com.consultorio.odonto.aplicacao.porta.entrada.TranscreverAudioUseCase;
import br.com.consultorio.odonto.aplicacao.porta.saida.ProvedorMidia;
import br.com.consultorio.odonto.aplicacao.porta.saida.TranscritorAudio;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.Optional;

/**
 * Baixa o áudio da Meta e o transcreve. Sem transcritor configurado, ou em caso de falha, devolve vazio e a
 * conversa pede que o paciente responda por texto.
 */
public final class TranscreverAudioService implements TranscreverAudioUseCase {

    private static final System.Logger LOG = System.getLogger(TranscreverAudioService.class.getName());

    private final ProvedorMidia provedorMidia;
    private final TranscritorAudio transcritor;

    public TranscreverAudioService(ProvedorMidia provedorMidia, TranscritorAudio transcritor) {
        this.provedorMidia = Objects.requireNonNull(provedorMidia);
        this.transcritor = Objects.requireNonNull(transcritor);
    }

    @Override
    public Optional<String> transcrever(String midiaId) {
        if (!transcritor.disponivel()) {
            return Optional.empty();
        }
        try {
            return transcritor.transcrever(provedorMidia.baixar(midiaId))
                    .map(String::strip)
                    .filter(texto -> !texto.isEmpty());
        } catch (FalhaIntegracaoException e) {
            LOG.log(Level.WARNING, "Não foi possível transcrever o áudio: {0}", e.getMessage());
            return Optional.empty();
        }
    }
}
