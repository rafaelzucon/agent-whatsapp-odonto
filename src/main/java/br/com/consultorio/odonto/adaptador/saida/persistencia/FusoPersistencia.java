package br.com.consultorio.odonto.adaptador.saida.persistencia;

import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;

/** Converte os horários locais do consultório (domínio) para instantes (timestamptz no banco) e vice-versa. */
@Component
class FusoPersistencia {

    private final Relogio relogio;

    FusoPersistencia(Relogio relogio) {
        this.relogio = relogio;
    }

    Instant paraInstante(LocalDateTime local) {
        return local.atZone(relogio.fuso()).toInstant();
    }

    LocalDateTime paraLocal(Instant instante) {
        return LocalDateTime.ofInstant(instante, relogio.fuso());
    }
}
