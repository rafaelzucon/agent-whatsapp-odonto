package br.com.consultorio.odonto.adaptador.entrada.web;

import br.com.consultorio.odonto.aplicacao.excecao.RecursoNaoEncontradoException;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ConsultarPacientesUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.PacienteVisao;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ProntuarioVisao;
import br.com.consultorio.odonto.aplicacao.porta.entrada.QueixaVisao;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ReclassificarQueixaUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Pacientes e prontuários para a equipe; inclui a reclassificação manual de queixas. */
@RestController
@RequestMapping("/api/admin")
public class PacienteController {

    private final ConsultarPacientesUseCase pacientes;
    private final ReclassificarQueixaUseCase reclassificar;

    public PacienteController(ConsultarPacientesUseCase pacientes, ReclassificarQueixaUseCase reclassificar) {
        this.pacientes = pacientes;
        this.reclassificar = reclassificar;
    }

    @GetMapping("/pacientes")
    public List<PacienteVisao> buscarPorNome(@RequestParam String nome) {
        return pacientes.buscarPorNome(nome);
    }

    @GetMapping("/pacientes/rg")
    public PacienteVisao buscarPorRg(@RequestParam String numero, @RequestParam String uf) {
        return pacientes.buscarPorRg(numero, uf)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nenhum paciente com esse RG."));
    }

    @GetMapping("/pacientes/{id}/prontuario")
    public ProntuarioVisao prontuario(@PathVariable long id) {
        return pacientes.prontuario(id);
    }

    @PutMapping("/queixas/{id}/problema")
    public QueixaVisao reclassificar(@PathVariable long id, @Valid @RequestBody ReclassificacaoRequest pedido) {
        return reclassificar.reclassificar(id, pedido.codigoProblema());
    }

    public record ReclassificacaoRequest(@NotBlank String codigoProblema) {
    }
}
