package br.com.consultorio.odonto.adaptador.entrada.web;

import br.com.consultorio.odonto.aplicacao.porta.entrada.AgendaVisao;
import br.com.consultorio.odonto.aplicacao.porta.entrada.BloqueioVisao;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ConsultaVisao;
import br.com.consultorio.odonto.aplicacao.porta.entrada.GerenciarAgendaUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/** Agenda do consultório para a equipe: consultas, horários livres, bloqueios e mudanças de status. */
@RestController
@RequestMapping("/api/admin/agenda")
public class AgendaController {

    private final GerenciarAgendaUseCase agenda;

    public AgendaController(GerenciarAgendaUseCase agenda) {
        this.agenda = agenda;
    }

    @GetMapping
    public AgendaVisao consultar(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> fim) {
        return agenda.consultar(inicio, fim.orElse(inicio));
    }

    @PostMapping("/consultas")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultaVisao agendar(@Valid @RequestBody NovaConsultaRequest pedido) {
        return agenda.agendarManualmente(pedido.pacienteId(), Optional.ofNullable(pedido.queixaId()), pedido.inicio());
    }

    @PostMapping("/consultas/{id}/cancelamento")
    public ConsultaVisao cancelar(@PathVariable long id) {
        return agenda.cancelar(id);
    }

    @PostMapping("/consultas/{id}/realizacao")
    public ConsultaVisao marcarRealizada(@PathVariable long id) {
        return agenda.marcarRealizada(id);
    }

    @PostMapping("/consultas/{id}/falta")
    public ConsultaVisao marcarFalta(@PathVariable long id) {
        return agenda.marcarFalta(id);
    }

    @PostMapping("/bloqueios")
    @ResponseStatus(HttpStatus.CREATED)
    public BloqueioVisao bloquear(@Valid @RequestBody NovoBloqueioRequest pedido) {
        return agenda.criarBloqueio(pedido.inicio(), pedido.fim(), pedido.motivo());
    }

    @DeleteMapping("/bloqueios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desbloquear(@PathVariable long id) {
        agenda.removerBloqueio(id);
    }

    public record NovaConsultaRequest(@NotNull Long pacienteId, Long queixaId, @NotNull LocalDateTime inicio) {
    }

    public record NovoBloqueioRequest(@NotNull LocalDateTime inicio, @NotNull LocalDateTime fim,
                                      @NotBlank @Size(max = 200) String motivo) {
    }
}
