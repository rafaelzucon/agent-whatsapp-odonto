package br.com.consultorio.odonto.aplicacao.casodeuso.interpretacao;

import br.com.consultorio.odonto.dominio.modelo.PreferenciaHorario;

import java.util.List;
import java.util.Optional;

/**
 * Tenta as estratégias na ordem recebida e usa a primeira que entender o texto (ex.: determinística antes da
 * IA). A ordem e as estratégias são definidas na composição, não aqui.
 */
public final class InterpretacaoEmCadeia implements InterpretadorPreferenciaHorario {

    private final List<InterpretadorPreferenciaHorario> estrategias;

    public InterpretacaoEmCadeia(List<InterpretadorPreferenciaHorario> estrategias) {
        if (estrategias.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos uma estratégia de interpretação.");
        }
        this.estrategias = List.copyOf(estrategias);
    }

    @Override
    public Optional<PreferenciaHorario> interpretar(String texto) {
        return estrategias.stream()
                .map(estrategia -> estrategia.interpretar(texto))
                .flatMap(Optional::stream)
                .findFirst();
    }
}
