package br.com.consultorio.odonto.adaptador.saida.ia;

/**
 * Prepara o texto do paciente para ir ao modelo como <em>dado</em>: limita o tamanho e neutraliza
 * tentativas de fechar a marcação que delimita o texto (defesa contra injeção de instruções).
 */
final class TextoParaIA {

    static final int TAMANHO_MAXIMO = 1_000;

    private TextoParaIA() {
    }

    static String delimitar(String marcador, String texto) {
        String limpo = texto == null ? "" : texto.strip();
        if (limpo.length() > TAMANHO_MAXIMO) {
            limpo = limpo.substring(0, TAMANHO_MAXIMO);
        }
        limpo = limpo.replace("<", "‹").replace(">", "›");
        return "<" + marcador + ">\n" + limpo + "\n</" + marcador + ">";
    }
}
