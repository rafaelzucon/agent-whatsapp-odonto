package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;

import java.util.Optional;

/** Converte áudio em texto. A transcrição é opcional: pode não haver provedor configurado. */
public interface TranscritorAudio {

    /** {@code true} se há um provedor de transcrição configurado. */
    boolean disponivel();

    /**
     * @return o texto transcrito, ou vazio se não houver provedor ou o áudio estiver sem fala; nunca {@code null}
     * @throws FalhaIntegracaoException em erro do provedor
     */
    Optional<String> transcrever(MidiaBaixada audio);
}
