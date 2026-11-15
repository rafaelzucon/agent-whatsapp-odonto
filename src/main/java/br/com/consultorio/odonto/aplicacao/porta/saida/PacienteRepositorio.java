package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.dominio.modelo.Paciente;

import java.util.List;
import java.util.Optional;

/** Persistência do agregado {@link Paciente}. Nunca devolve {@code null}. */
public interface PacienteRepositorio {

    /**
     * Grava o paciente (novo ou alterado) e devolve a versão gravada, com id.
     * O adaptador calcula o hash de busca do RG e cifra os dados pessoais.
     */
    Paciente salvar(Paciente paciente);

    Optional<Paciente> buscarPorId(long id);

    /** Busca pelo hash do RG calculado por {@link CifradorDocumento#hash}. */
    Optional<Paciente> buscarPorRgHash(String rgHash);

    /** Todos os pacientes (os nomes são cifrados, então a busca por nome é feita em memória). */
    List<Paciente> listarTodos();
}
