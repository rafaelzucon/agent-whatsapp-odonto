package br.com.consultorio.odonto.aplicacao.porta.entrada;

import java.util.Optional;

/** Baixa e transcreve o áudio enviado pelo paciente. */
public interface TranscreverAudioUseCase {

    /** @return o texto, ou vazio se não houver transcrição disponível ou ela falhar */
    Optional<String> transcrever(String midiaId);
}
