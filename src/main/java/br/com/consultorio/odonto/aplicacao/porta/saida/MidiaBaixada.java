package br.com.consultorio.odonto.aplicacao.porta.saida;

import java.util.Objects;

/** Arquivo de mídia baixado do WhatsApp (ex.: áudio OGG/Opus). */
public record MidiaBaixada(byte[] conteudo, String mimeType) {

    public MidiaBaixada {
        Objects.requireNonNull(conteudo, "conteudo");
        Objects.requireNonNull(mimeType, "mimeType");
        conteudo = conteudo.clone();
    }

    @Override
    public byte[] conteudo() {
        return conteudo.clone();
    }

    /** Extensão de arquivo coerente com o tipo (os provedores de transcrição usam o nome do arquivo). */
    public String extensao() {
        String tipo = mimeType.toLowerCase(java.util.Locale.ROOT);
        if (tipo.contains("ogg") || tipo.contains("opus")) {
            return "ogg";
        }
        if (tipo.contains("mpeg") || tipo.contains("mp3")) {
            return "mp3";
        }
        if (tipo.contains("mp4") || tipo.contains("m4a") || tipo.contains("aac")) {
            return "m4a";
        }
        if (tipo.contains("amr")) {
            return "amr";
        }
        return "bin";
    }
}
