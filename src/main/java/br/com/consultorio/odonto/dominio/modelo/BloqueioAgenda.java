package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;

import java.util.Objects;
import java.util.Optional;

/**
 * Período em que a agenda não aceita consultas (férias, cursos, manutenção).
 *
 * @param id vazio enquanto o bloqueio não foi persistido
 */
public record BloqueioAgenda(Optional<Long> id, Horario periodo, String motivo) {

    public BloqueioAgenda {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(periodo, "periodo");
        if (motivo == null || motivo.isBlank()) {
            throw new DadoInvalidoException("Informe o motivo do bloqueio.");
        }
        motivo = motivo.trim();
    }

    public static BloqueioAgenda novo(Horario periodo, String motivo) {
        return new BloqueioAgenda(Optional.empty(), periodo, motivo);
    }

    public boolean bloqueia(Horario horario) {
        return periodo.sobrepoe(horario);
    }
}
