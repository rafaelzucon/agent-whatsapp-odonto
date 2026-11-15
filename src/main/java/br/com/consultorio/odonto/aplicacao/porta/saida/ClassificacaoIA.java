package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.dominio.modelo.Prioridade;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resposta do classificador de IA.
 *
 * @param codigo        código do catálogo escolhido (pode não existir; o caso de uso confere)
 * @param confianca     de 0 a 1
 * @param sintomas      sintomas citados pelo paciente, como a IA entendeu
 * @param sinaisAlarme  se a IA percebeu sinais de alarme (eleva a prioridade para EMERGENCIA)
 * @param prioridade    prioridade sugerida pela IA, se houver
 */
public record ClassificacaoIA(String codigo, double confianca, List<String> sintomas, boolean sinaisAlarme,
                              Optional<Prioridade> prioridade) {

    public ClassificacaoIA {
        Objects.requireNonNull(codigo, "codigo");
        sintomas = List.copyOf(sintomas);
        Objects.requireNonNull(prioridade, "prioridade");
    }

    /** Prioridade indicada pela IA: EMERGENCIA se houver sinais de alarme; senão a sugerida, ou ROTINA. */
    public Prioridade prioridadeIndicada() {
        return sinaisAlarme ? Prioridade.EMERGENCIA : prioridade.orElse(Prioridade.ROTINA);
    }
}
