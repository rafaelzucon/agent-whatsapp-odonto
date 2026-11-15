package br.com.consultorio.odonto.aplicacao.porta.entrada;

import br.com.consultorio.odonto.dominio.modelo.DocumentoRg;
import br.com.consultorio.odonto.dominio.modelo.WaId;

import java.time.LocalDate;
import java.util.Optional;

/** Cadastra o paciente (com consentimento LGPD) e abre o prontuário na mesma transação. */
public interface CadastrarPacienteUseCase {

    PacienteCadastrado cadastrar(CadastrarPacienteCommand comando);

    record CadastrarPacienteCommand(DocumentoRg rg, String nomeCompleto, LocalDate dataNascimento,
                                    Optional<String> responsavelLegal, WaId telefone) {
    }

    record PacienteCadastrado(long pacienteId, String primeiroNome, String numeroProntuario) {
    }
}
