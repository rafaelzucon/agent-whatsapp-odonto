package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.dominio.modelo.Prontuario;

import java.util.Optional;

/** Persistência do agregado {@link Prontuario} com suas queixas. Nunca devolve {@code null}. */
public interface ProntuarioRepositorio {

    /** Grava o prontuário e as queixas; devolve a versão gravada, com ids, queixas em ordem de registro. */
    Prontuario salvar(Prontuario prontuario);

    /** Próximo número sequencial do prontuário (sequência do banco, sem repetição). */
    long proximoSequencial();

    Optional<Prontuario> buscarPorPacienteId(long pacienteId);

    Optional<Prontuario> buscarPorQueixaId(long queixaId);
}
