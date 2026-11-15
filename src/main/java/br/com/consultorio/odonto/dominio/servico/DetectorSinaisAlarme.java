package br.com.consultorio.odonto.dominio.servico;

import br.com.consultorio.odonto.dominio.modelo.Prioridade;

import java.util.Collection;
import java.util.List;

/**
 * Detecta sinais de alarme (possível emergência) no texto da queixa, por palavras-chave normalizadas
 * (sem acento, minúsculas). Quando acionado, a prioridade é EMERGENCIA, independentemente da IA.
 */
public final class DetectorSinaisAlarme {

    public static final List<String> PALAVRAS_PADRAO = List.of(
            "falta de ar", "dificuldade para respirar", "dificuldade de respirar", "nao consigo respirar",
            "dificuldade para engolir", "dificuldade de engolir", "nao consigo engolir",
            "rosto inchado", "rosto todo inchado", "cara inchada", "inchaco no rosto", "inchaco no pescoco",
            "pescoco inchado", "olho inchado", "febre alta", "sangramento que nao para", "sangue que nao para",
            "nao para de sangrar", "dente caiu", "dente arrancado", "arrancou o dente", "bati o dente",
            "quebrei o dente num acidente", "acidente", "trauma", "pancada na boca");

    private final List<String> palavras;

    public DetectorSinaisAlarme(Collection<String> palavras) {
        this.palavras = palavras == null || palavras.isEmpty()
                ? PALAVRAS_PADRAO.stream().map(NormalizadorTexto::normalizar).toList()
                : palavras.stream().map(NormalizadorTexto::normalizar).filter(p -> !p.isEmpty()).distinct().toList();
    }

    public DetectorSinaisAlarme() {
        this(PALAVRAS_PADRAO);
    }

    /** Lista os sinais de alarme encontrados no texto (vazia se nenhum). */
    public List<String> sinaisEncontrados(String texto) {
        String normalizado = NormalizadorTexto.normalizar(texto);
        return palavras.stream().filter(p -> NormalizadorTexto.contemExpressao(normalizado, p)).toList();
    }

    public boolean possuiSinaisAlarme(String texto) {
        return !sinaisEncontrados(texto).isEmpty();
    }

    /** EMERGENCIA se houver sinais de alarme; ROTINA caso contrário (não eleva a prioridade). */
    public Prioridade prioridadePara(String texto) {
        return possuiSinaisAlarme(texto) ? Prioridade.EMERGENCIA : Prioridade.ROTINA;
    }
}
