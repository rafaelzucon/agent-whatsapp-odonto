package br.com.consultorio.odonto.adaptador.saida.persistencia;

import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;

/** Converte os horários locais do consultório (domínio) para instantes (timestamptz no PostgreSQL, texto ISO-8601 no DynamoDB) e vice-versa. */
@Component
public class FusoPersistencia {

    private final Relogio relogio;

    public FusoPersistencia(Relogio relogio) {
        this.relogio = relogio;
    }

    public Instant paraInstante(LocalDateTime local) {
        return local.atZone(relogio.fuso()).toInstant();
    }

    public LocalDateTime paraLocal(Instant instante) {
        return LocalDateTime.ofInstant(instante, relogio.fuso());
    }
}
