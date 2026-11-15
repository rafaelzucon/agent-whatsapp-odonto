package br.com.consultorio.odonto.dominio.servico;

import br.com.consultorio.odonto.dominio.modelo.Periodo;
import br.com.consultorio.odonto.dominio.modelo.PreferenciaHorario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class InterpretadorDataHoraDeterministicoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 16, 9, 0);
    private final InterpretadorDataHoraDeterministico interpretador = new InterpretadorDataHoraDeterministico();

    @Test
    void dataEHoraExplicitas() {
        assertThat(interpretador.interpretar("25/11 às 10h", AGORA))
                .contains(PreferenciaHorario.de(LocalDate.of(2026, 11, 25), LocalTime.of(10, 0), null));
        assertThat(interpretador.interpretar("25/11/2026 10:30", AGORA))
                .contains(PreferenciaHorario.de(LocalDate.of(2026, 11, 25), LocalTime.of(10, 30), null));
    }

    @Test
    void dataSemAnoNoPassadoVaiParaOAnoSeguinte() {
        assertThat(interpretador.interpretar("10/01 14h", AGORA))
                .contains(PreferenciaHorario.de(LocalDate.of(2027, 1, 10), LocalTime.of(14, 0), null));
    }

    @Test
    void expressoesRelativas() {
        assertThat(interpretador.interpretar("amanhã 14h", AGORA))
                .contains(PreferenciaHorario.de(LocalDate.of(2026, 11, 17), LocalTime.of(14, 0), null));
        assertThat(interpretador.interpretar("hoje 16h", AGORA))
                .contains(PreferenciaHorario.de(LocalDate.of(2026, 11, 16), LocalTime.of(16, 0), null));
        assertThat(interpretador.interpretar("depois de amanhã de manhã", AGORA))
                .contains(PreferenciaHorario.de(LocalDate.of(2026, 11, 18), null, Periodo.MANHA));
        assertThat(interpretador.interpretar("amanhã à tarde", AGORA))
                .contains(PreferenciaHorario.de(LocalDate.of(2026, 11, 17), null, Periodo.TARDE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sexta às 10h", "próxima segunda", "dia 25 às 10h", "qualquer horário", "31/02 10h"})
    void textosQueExigemIaDevolvemVazio(String texto) {
        assertThat(interpretador.interpretar(texto, AGORA)).isEmpty();
    }
}
