package br.com.consultorio.odonto.adaptador.saida.transcricao;

import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;
import br.com.consultorio.odonto.aplicacao.porta.saida.MidiaBaixada;
import br.com.consultorio.odonto.aplicacao.porta.saida.TranscritorAudio;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Transcreve áudios com o modelo de transcrição configurado no Spring AI (OpenAI Whisper por padrão).
 * Opcional: com {@code TRANSCRICAO_PROVEDOR=none} não há modelo e a conversa pede resposta por texto.
 */
@Component
public class WhisperTranscritorAdapter implements TranscritorAudio {

    private final ObjectProvider<TranscriptionModel> modelo;

    public WhisperTranscritorAdapter(ObjectProvider<TranscriptionModel> modelo) {
        this.modelo = modelo;
    }

    @Override
    public boolean disponivel() {
        return modelo.getIfAvailable() != null;
    }

    @Override
    public Optional<String> transcrever(MidiaBaixada audio) {
        TranscriptionModel transcritor = modelo.getIfAvailable();
        if (transcritor == null) {
            return Optional.empty();
        }
        String nomeArquivo = "audio." + audio.extensao();
        ByteArrayResource arquivo = new ByteArrayResource(audio.conteudo()) {
            @Override
            public String getFilename() {
                return nomeArquivo;
            }
        };
        try {
            AudioTranscriptionResponse resposta = transcritor.call(new AudioTranscriptionPrompt(arquivo));
            String texto = resposta == null || resposta.getResult() == null ? null : resposta.getResult().getOutput();
            return Optional.ofNullable(texto).map(String::strip).filter(t -> !t.isEmpty());
        } catch (RuntimeException e) {
            throw new FalhaIntegracaoException("Falha ao transcrever o áudio: " + e.getMessage(), e);
        }
    }
}
