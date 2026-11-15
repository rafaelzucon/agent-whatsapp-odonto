package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Intervalo de tempo [inicio, fim) no horário local do consultório.
 */
public record Horario(LocalDateTime inicio, LocalDateTime fim) {

    public Horario {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fim, "fim");
        if (!fim.isAfter(inicio)) {
            throw new DadoInvalidoException("O fim do horário deve ser depois do início.");
        }
    }

    public static Horario de(LocalDateTime inicio, Duration duracao) {
        return new Horario(inicio, inicio.plus(duracao));
    }

    public boolean sobrepoe(Horario outro) {
        return inicio.isBefore(outro.fim) && outro.inicio.isBefore(fim);
    }

    public LocalDate data() {
        return inicio.toLocalDate();
    }

    public Duration duracao() {
        return Duration.between(inicio, fim);
    }
}
