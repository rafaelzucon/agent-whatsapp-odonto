package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;

/**
 * O que o paciente pediu: data, hora e/ou período. Ao menos um deles está presente.
 */
public record PreferenciaHorario(Optional<LocalDate> data, Optional<LocalTime> hora, Optional<Periodo> periodo) {

    public PreferenciaHorario {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(hora, "hora");
        Objects.requireNonNull(periodo, "periodo");
        if (data.isEmpty() && hora.isEmpty() && periodo.isEmpty()) {
            throw new DadoInvalidoException("A preferência precisa de data, hora ou período.");
        }
    }

    public static PreferenciaHorario de(LocalDate data, LocalTime hora, Periodo periodo) {
        return new PreferenciaHorario(Optional.ofNullable(data), Optional.ofNullable(hora), Optional.ofNullable(periodo));
    }

    public boolean ehHorarioExato() {
        return data.isPresent() && hora.isPresent();
    }
}
