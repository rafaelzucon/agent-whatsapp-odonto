package br.com.consultorio.odonto.adaptador.saida.plataforma;

import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

/** Relógio real, no fuso do consultório. Criado pela configuração a partir da propriedade de fuso. */
public class RelogioSistema implements Relogio {

    private final Clock relogio;

    public RelogioSistema(ZoneId fuso) {
        this(Clock.system(Objects.requireNonNull(fuso, "fuso")));
    }

    RelogioSistema(Clock relogio) {
        this.relogio = relogio;
    }

    @Override
    public Instant agora() {
        return relogio.instant();
    }

    @Override
    public ZoneId fuso() {
        return relogio.getZone();
    }
}
