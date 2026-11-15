package br.com.consultorio.odonto.aplicacao.porta.entrada;

import java.util.List;
import java.util.Optional;

/** Busca de pacientes e consulta do prontuário. */
public interface ConsultarPacientesUseCase {

    /** Busca por parte do nome, sem diferenciar acentos e maiúsculas (mínimo de 2 letras). */
    List<PacienteVisao> buscarPorNome(String nome);

    Optional<PacienteVisao> buscarPorRg(String rg, String uf);

    /** Lança {@code RecursoNaoEncontradoException} se o paciente não existir. */
    ProntuarioVisao prontuario(long pacienteId);
}
