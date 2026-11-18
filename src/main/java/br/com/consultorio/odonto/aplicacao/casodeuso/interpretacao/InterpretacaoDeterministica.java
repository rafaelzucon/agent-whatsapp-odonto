package br.com.consultorio.odonto.aplicacao.casodeuso.interpretacao;

import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import br.com.consultorio.odonto.dominio.modelo.PreferenciaHorario;
import br.com.consultorio.odonto.dominio.servico.InterpretadorDataHoraDeterministico;

import java.util.Objects;
import java.util.Optional;

/** Interpretação por regras fixas do domínio: rápida, previsível e sem custo. */
public final class InterpretacaoDeterministica implements InterpretadorPreferenciaHorario {

    private final InterpretadorDataHoraDeterministico interpretador;
    private final Relogio relogio;

    public InterpretacaoDeterministica(InterpretadorDataHoraDeterministico interpretador, Relogio relogio) {
        this.interpretador = Objects.requireNonNull(interpretador);
        this.relogio = Objects.requireNonNull(relogio);
    }

    @Override
    public Optional<PreferenciaHorario> interpretar(String texto) {
        return interpretador.interpretar(texto, relogio.agoraLocal());
    }
}
