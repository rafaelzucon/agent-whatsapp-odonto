package br.com.consultorio.odonto.adaptador.saida.transcricao;

import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;
import br.com.consultorio.odonto.aplicacao.porta.saida.MidiaBaixada;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WhisperTranscritorAdapterTest {

    private static final MidiaBaixada AUDIO = new MidiaBaixada(new byte[]{1, 2}, "audio/ogg; codecs=opus");

    private static ObjectProvider<TranscriptionModel> provedor(TranscriptionModel modelo) {
        StaticListableBeanFactory fabrica = new StaticListableBeanFactory();
        if (modelo != null) {
            fabrica.addBean("transcricao", modelo);
        }
        return fabrica.getBeanProvider(TranscriptionModel.class);
    }

    @Test
    void semModeloNaoTranscreve() {
        WhisperTranscritorAdapter adaptador = new WhisperTranscritorAdapter(provedor(null));

        assertThat(adaptador.disponivel()).isFalse();
        assertThat(adaptador.transcrever(AUDIO)).isEmpty();
    }

    @Test
    void transcreveComNomeDeArquivoCoerente() {
        TranscriptionModel modelo = mock(TranscriptionModel.class);
        when(modelo.call(any(AudioTranscriptionPrompt.class)))
                .thenReturn(new AudioTranscriptionResponse(new AudioTranscription("  Estou com dor de dente  ")));
        WhisperTranscritorAdapter adaptador = new WhisperTranscritorAdapter(provedor(modelo));

        assertThat(adaptador.disponivel()).isTrue();
        assertThat(adaptador.transcrever(AUDIO)).contains("Estou com dor de dente");
        ArgumentCaptor<AudioTranscriptionPrompt> prompt = ArgumentCaptor.forClass(AudioTranscriptionPrompt.class);
        verify(modelo).call(prompt.capture());
        assertThat(prompt.getValue().getInstructions().getFilename()).isEqualTo("audio.ogg");
    }

    @Test
    void audioSemFalaOuErro() {
        TranscriptionModel modelo = mock(TranscriptionModel.class);
        when(modelo.call(any(AudioTranscriptionPrompt.class)))
                .thenReturn(new AudioTranscriptionResponse(new AudioTranscription("  ")))
                .thenThrow(new IllegalStateException("401"));
        WhisperTranscritorAdapter adaptador = new WhisperTranscritorAdapter(provedor(modelo));

        assertThat(adaptador.transcrever(AUDIO)).isEmpty();
        assertThatThrownBy(() -> adaptador.transcrever(AUDIO)).isInstanceOf(FalhaIntegracaoException.class);
    }
}
