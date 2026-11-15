package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;
import br.com.consultorio.odonto.aplicacao.excecao.RecursoNaoEncontradoException;
import br.com.consultorio.odonto.aplicacao.porta.entrada.AgendaVisao;
import br.com.consultorio.odonto.aplicacao.porta.entrada.CadastrarPacienteUseCase.CadastrarPacienteCommand;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ConsultaVisao;
import br.com.consultorio.odonto.dominio.excecao.HorarioIndisponivelException;
import br.com.consultorio.odonto.dominio.modelo.DocumentoRg;
import br.com.consultorio.odonto.dominio.modelo.WaId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GerenciarAgendaServiceTest {

    private static final LocalDateTime QUARTA_10H = LocalDateTime.of(2026, 11, 18, 10, 0);

    private final Cenario cenario = new Cenario();
    private final GerenciarAgendaService agenda = new GerenciarAgendaService(cenario.consultas, cenario.bloqueios,
            cenario.pacientes, cenario.prontuarios, cenario.agendar, cenario.visoes, cenario.configuracaoAgenda,
            cenario.transacao, cenario.relogio);
    private long pacienteId;

    @BeforeEach
    void cadastrar() {
        pacienteId = cenario.cadastrar.cadastrar(new CadastrarPacienteCommand(DocumentoRg.de("123456789", "SP"),
                "Maria da Silva", LocalDate.of(1990, 5, 10), Optional.empty(), new WaId(Cenario.PACIENTE)))
                .pacienteId();
    }

    @Test
    @DisplayName("agenda manualmente, lista na agenda e não avisa o consultório")
    void agendaManualmente() {
        ConsultaVisao consulta = agenda.agendarManualmente(pacienteId, Optional.empty(), QUARTA_10H);

        AgendaVisao dia = agenda.consultar(QUARTA_10H.toLocalDate(), QUARTA_10H.toLocalDate());
        assertThat(dia.consultas()).extracting(ConsultaVisao::id).containsExactly(consulta.id());
        assertThat(dia.horariosLivres()).noneMatch(h -> h.inicio().equals(QUARTA_10H));
        assertThat(cenario.publicador.eventos).isEmpty();
    }

    @Test
    @DisplayName("agendamento manual em horário ocupado é recusado")
    void horarioOcupado() {
        agenda.agendarManualmente(pacienteId, Optional.empty(), QUARTA_10H);

        assertThatThrownBy(() -> agenda.agendarManualmente(pacienteId, Optional.empty(), QUARTA_10H))
                .isInstanceOf(HorarioIndisponivelException.class);
    }

    @Test
    @DisplayName("cancelar libera o horário")
    void cancelarLiberaHorario() {
        ConsultaVisao consulta = agenda.agendarManualmente(pacienteId, Optional.empty(), QUARTA_10H);

        assertThat(agenda.cancelar(consulta.id()).status()).isEqualTo("CANCELADA");
        assertThat(agenda.agendarManualmente(pacienteId, Optional.empty(), QUARTA_10H).inicio()).isEqualTo(QUARTA_10H);
    }

    @Test
    @DisplayName("período acima do máximo ou invertido é inválido")
    void periodoInvalido() {
        LocalDate hoje = LocalDate.of(2026, 11, 16);
        assertThatThrownBy(() -> agenda.consultar(hoje, hoje.plusDays(92)))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> agenda.consultar(hoje, hoje.minusDays(1)))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    @DisplayName("recursos inexistentes geram RecursoNaoEncontradoException")
    void recursosInexistentes() {
        assertThatThrownBy(() -> agenda.removerBloqueio(999)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> agenda.cancelar(999)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> agenda.agendarManualmente(999, Optional.empty(), QUARTA_10H))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("bloqueio tira os horários da agenda")
    void bloqueioTiraHorarios() {
        agenda.criarBloqueio(QUARTA_10H, QUARTA_10H.plusHours(2), "Curso");

        assertThatThrownBy(() -> agenda.agendarManualmente(pacienteId, Optional.empty(), QUARTA_10H.plusMinutes(30)))
                .isInstanceOf(HorarioIndisponivelException.class);
    }
}
