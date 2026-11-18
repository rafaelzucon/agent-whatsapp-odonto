package br.com.consultorio.odonto.aplicacao.casodeuso.interpretacao;

import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;
import br.com.consultorio.odonto.aplicacao.porta.saida.InterpretadorDataHoraIA;
import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import br.com.consultorio.odonto.dominio.modelo.PreferenciaHorario;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.Optional;

/** Interpretação pela IA (porta de saída). Se a IA estiver indisponível, devolve vazio em vez de falhar. */
public final class InterpretacaoPorIA implements InterpretadorPreferenciaHorario {

    private static final System.Logger LOG = System.getLogger(InterpretacaoPorIA.class.getName());

    private final InterpretadorDataHoraIA interpretadorIA;
    private final Relogio relogio;

    public InterpretacaoPorIA(InterpretadorDataHoraIA interpretadorIA, Relogio relogio) {
        this.interpretadorIA = Objects.requireNonNull(interpretadorIA);
        this.relogio = Objects.requireNonNull(relogio);
    }

    @Override
    public Optional<PreferenciaHorario> interpretar(String texto) {
        try {
            return interpretadorIA.interpretar(texto, relogio.agoraLocal(), relogio.fuso());
        } catch (FalhaIntegracaoException e) {
            LOG.log(Level.WARNING, "Interpretação de data por IA indisponível: {0}", e.getMessage());
            return Optional.empty();
        }
    }
}
