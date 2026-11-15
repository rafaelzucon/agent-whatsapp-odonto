package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.excecao.TransicaoInvalidaException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultaTest {

    private static final Horario HORARIO = Horario.de(LocalDateTime.of(2026, 11, 18, 10, 0), Duration.ofMinutes(30));
    private static final Instant AGORA = Instant.parse("2026-11-16T12:00:00Z");

    private final Consulta consulta = Consulta.agendar(1L, Optional.of(2L), HORARIO, AGORA);

    @Test
    void novaConsultaFicaAgendada() {
        assertThat(consulta.status()).isEqualTo(StatusConsulta.AGENDADA);
        assertThat(consulta.estaAgendada()).isTrue();
    }

    @Test
    void cancelarLiberaOHorario() {
        consulta.cancelar();

        assertThat(consulta.status()).isEqualTo(StatusConsulta.CANCELADA);
        assertThat(consulta.ocupa(HORARIO)).isFalse();
        assertThatThrownBy(consulta::cancelar).isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    void naoPodeMarcarRealizadaAntesDoInicio() {
        assertThatThrownBy(() -> consulta.marcarRealizada(HORARIO.inicio().minusMinutes(1)))
                .isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    void realizadaOuFaltaSoAPartirDeAgendada() {
        consulta.marcarFalta(HORARIO.inicio().plusMinutes(5));

        assertThat(consulta.status()).isEqualTo(StatusConsulta.FALTOU);
        assertThatThrownBy(() -> consulta.marcarRealizada(HORARIO.fim()))
                .isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    void ocupaHorarioSobreposto() {
        Horario sobreposto = Horario.de(HORARIO.inicio().plusMinutes(15), Duration.ofMinutes(30));

        assertThat(consulta.ocupa(sobreposto)).isTrue();
    }
}
