package br.com.consultorio.odonto.dominio.servico;

import br.com.consultorio.odonto.dominio.excecao.HorarioIndisponivelException;
import br.com.consultorio.odonto.dominio.excecao.MotivoIndisponibilidade;
import br.com.consultorio.odonto.dominio.modelo.BloqueioAgenda;
import br.com.consultorio.odonto.dominio.modelo.Consulta;
import br.com.consultorio.odonto.dominio.modelo.Disponibilidade;
import br.com.consultorio.odonto.dominio.modelo.Horario;
import br.com.consultorio.odonto.dominio.modelo.HorarioFuncionamento;
import br.com.consultorio.odonto.dominio.modelo.Periodo;
import br.com.consultorio.odonto.dominio.modelo.PreferenciaHorario;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgendaTest {

    /** Segunda-feira, 16/11/2026, 09:00. */
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 16, 9, 0);
    private static final LocalDate TERCA = LocalDate.of(2026, 11, 17);
    private static final LocalDate QUARTA = LocalDate.of(2026, 11, 18);

    private static final HorarioFuncionamento EXPEDIENTE = new HorarioFuncionamento(
            EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), LocalTime.of(8, 0), LocalTime.of(18, 0),
            LocalTime.of(12, 0), LocalTime.of(13, 0), Duration.ofMinutes(30), Duration.ofHours(2));

    private static Agenda agenda(Set<LocalDate> feriados, List<BloqueioAgenda> bloqueios, List<Consulta> consultas,
                                 int diasBusca) {
        return new Agenda(EXPEDIENTE, feriados, bloqueios, consultas, diasBusca);
    }

    private static Agenda agendaVazia() {
        return agenda(Set.of(), List.of(), List.of(), 60);
    }

    private static Consulta consultaEm(LocalDateTime inicio) {
        return Consulta.agendar(1L, Optional.empty(), Horario.de(inicio, Duration.ofMinutes(30)), Instant.EPOCH);
    }

    private static Horario meiaHora(LocalDateTime inicio) {
        return Horario.de(inicio, Duration.ofMinutes(30));
    }

    @Test
    void horarioDentroDoExpedienteELivreEstaDisponivel() {
        LocalDateTime pedido = TERCA.atTime(10, 0);

        assertThat(agendaVazia().verificar(pedido, AGORA)).isEmpty();
        assertThat(agendaVazia().resolver(PreferenciaHorario.de(TERCA, LocalTime.of(10, 0), null), AGORA))
                .isEqualTo(new Disponibilidade.HorarioDisponivel(meiaHora(pedido)));
    }

    @Test
    void foraDoExpedienteNaoPodeSerAgendado() {
        assertThat(agendaVazia().verificar(TERCA.atTime(7, 30), AGORA)).contains(MotivoIndisponibilidade.FORA_DO_EXPEDIENTE);
        assertThat(agendaVazia().verificar(TERCA.atTime(18, 0), AGORA)).contains(MotivoIndisponibilidade.FORA_DO_EXPEDIENTE);
        assertThat(agendaVazia().verificar(TERCA.atTime(17, 30), AGORA)).isEmpty();
    }

    @Test
    void intervaloDeAlmocoNaoPodeSerAgendado() {
        assertThat(agendaVazia().verificar(TERCA.atTime(12, 0), AGORA)).contains(MotivoIndisponibilidade.INTERVALO_ALMOCO);
        assertThat(agendaVazia().verificar(TERCA.atTime(13, 0), AGORA)).isEmpty();
    }

    @Test
    void feriadoEFimDeSemanaNaoTemAtendimento() {
        LocalDate sexta = LocalDate.of(2026, 11, 20);
        Agenda agenda = agenda(Set.of(sexta), List.of(), List.of(), 60);

        assertThat(agenda.verificar(sexta.atTime(10, 0), AGORA)).contains(MotivoIndisponibilidade.FERIADO);
        assertThat(agenda.verificar(LocalDate.of(2026, 11, 21).atTime(10, 0), AGORA))
                .contains(MotivoIndisponibilidade.DIA_SEM_ATENDIMENTO);
    }

    @Test
    void bloqueioImpedeAgendamento() {
        BloqueioAgenda curso = BloqueioAgenda.novo(new Horario(TERCA.atTime(8, 0), TERCA.atTime(12, 0)), "Curso");
        Agenda agenda = agenda(Set.of(), List.of(curso), List.of(), 60);

        assertThat(agenda.verificar(TERCA.atTime(9, 0), AGORA)).contains(MotivoIndisponibilidade.BLOQUEADO);
        assertThat(agenda.primeiroLivreNoDia(TERCA, AGORA)).contains(meiaHora(TERCA.atTime(13, 0)));
    }

    @Test
    void horarioOcupadoSugereOProximoLivre() {
        Agenda agenda = agenda(Set.of(), List.of(), List.of(consultaEm(TERCA.atTime(10, 0))), 60);

        assertThat(agenda.verificar(TERCA.atTime(10, 0), AGORA)).contains(MotivoIndisponibilidade.OCUPADO);
        assertThat(agenda.resolver(PreferenciaHorario.de(TERCA, LocalTime.of(10, 0), null), AGORA))
                .isEqualTo(new Disponibilidade.HorarioSugerido(meiaHora(TERCA.atTime(10, 30)), true));
    }

    @Test
    void horarioForaDaGradeSugereOProximoInicioDaGrade() {
        assertThat(agendaVazia().verificar(TERCA.atTime(10, 10), AGORA)).contains(MotivoIndisponibilidade.FORA_DA_GRADE);
        assertThat(agendaVazia().resolver(PreferenciaHorario.de(TERCA, LocalTime.of(10, 10), null), AGORA))
                .isEqualTo(new Disponibilidade.HorarioSugerido(meiaHora(TERCA.atTime(10, 30)), true));
    }

    @Test
    void somenteDataOfereceOPrimeiroHorarioLivreDoDia() {
        Agenda agenda = agenda(Set.of(), List.of(), List.of(consultaEm(TERCA.atTime(8, 0))), 60);

        assertThat(agenda.resolver(PreferenciaHorario.de(TERCA, null, null), AGORA))
                .isEqualTo(new Disponibilidade.HorarioSugerido(meiaHora(TERCA.atTime(8, 30)), false));
    }

    @Test
    void somentePeriodoOfereceOPrimeiroLivreNoPeriodoAPartirDeHoje() {
        assertThat(agendaVazia().resolver(PreferenciaHorario.de(null, null, Periodo.TARDE), AGORA))
                .isEqualTo(new Disponibilidade.HorarioSugerido(meiaHora(AGORA.toLocalDate().atTime(13, 0)), false));
    }

    @Test
    void diaCheioSugereODiaSeguinte() {
        List<Consulta> diaInteiro = new ArrayList<>();
        EXPEDIENTE.grade(TERCA).forEach(inicio -> diaInteiro.add(consultaEm(inicio)));
        Agenda agenda = agenda(Set.of(), List.of(), diaInteiro, 60);

        assertThat(agenda.resolver(PreferenciaHorario.de(TERCA, null, null), AGORA))
                .isEqualTo(new Disponibilidade.HorarioSugerido(meiaHora(QUARTA.atTime(8, 0)), true));
        assertThat(agenda.resolver(PreferenciaHorario.de(TERCA, LocalTime.of(15, 0), null), AGORA))
                .isEqualTo(new Disponibilidade.HorarioSugerido(meiaHora(QUARTA.atTime(8, 0)), true));
    }

    @Test
    void respeitaAAntecedenciaMinima() {
        LocalDateTime daquiAUmaHora = AGORA.plusHours(1);

        assertThat(agendaVazia().verificar(daquiAUmaHora, AGORA)).contains(MotivoIndisponibilidade.ANTECEDENCIA_MINIMA);
        assertThat(agendaVazia().verificar(AGORA.minusHours(1), AGORA)).contains(MotivoIndisponibilidade.NO_PASSADO);
        assertThat(agendaVazia().resolver(PreferenciaHorario.de(AGORA.toLocalDate(), LocalTime.of(10, 0), null), AGORA))
                .isEqualTo(new Disponibilidade.HorarioSugerido(meiaHora(AGORA.toLocalDate().atTime(11, 0)), true));
    }

    @Test
    void respeitaOLimiteDeBusca() {
        Agenda agenda = agenda(Set.of(), List.of(
                BloqueioAgenda.novo(new Horario(AGORA, TERCA.atTime(18, 0)), "Férias")), List.of(), 1);

        assertThat(agenda.verificar(QUARTA.atTime(10, 0), AGORA)).contains(MotivoIndisponibilidade.ALEM_DO_LIMITE_DE_BUSCA);
        assertThat(agenda.primeiroLivre(AGORA)).isEqualTo(new Disponibilidade.SemHorarioLivre());
    }

    @Test
    void semHorarioDepoisSugereOMaisProximoAntesNoMesmoDia() {
        Agenda agenda = agenda(Set.of(), List.of(), List.of(consultaEm(TERCA.atTime(17, 30))), 1);

        assertThat(agenda.resolver(PreferenciaHorario.de(TERCA, LocalTime.of(17, 30), null), AGORA))
                .isEqualTo(new Disponibilidade.HorarioSugerido(meiaHora(TERCA.atTime(17, 0)), true));
    }

    @Test
    void validarLancaExcecaoComOMotivo() {
        assertThatThrownBy(() -> agendaVazia().validar(TERCA.atTime(12, 30), AGORA))
                .isInstanceOf(HorarioIndisponivelException.class)
                .extracting(e -> ((HorarioIndisponivelException) e).motivo())
                .isEqualTo(MotivoIndisponibilidade.INTERVALO_ALMOCO);
    }

    @Test
    void consultaCanceladaNaoOcupaOHorario() {
        Consulta cancelada = consultaEm(TERCA.atTime(10, 0));
        cancelada.cancelar();

        assertThat(agenda(Set.of(), List.of(), List.of(cancelada), 60).estaLivre(TERCA.atTime(10, 0), AGORA)).isTrue();
    }

    @Test
    void listaOsHorariosLivresDoDia() {
        Agenda agenda = agenda(Set.of(), List.of(), List.of(consultaEm(TERCA.atTime(8, 0))), 60);

        List<Horario> livres = agenda.horariosLivres(TERCA, TERCA, AGORA);

        assertThat(livres).hasSize(EXPEDIENTE.grade(TERCA).size() - 1);
        assertThat(livres.getFirst().inicio()).isEqualTo(TERCA.atTime(8, 30));
    }
}
