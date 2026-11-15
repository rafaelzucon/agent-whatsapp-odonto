package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.dominio.modelo.BloqueioAgenda;

import java.time.LocalDateTime;
import java.util.List;

/** Bloqueios da agenda (férias, cursos). Nunca devolve {@code null}. */
public interface BloqueioAgendaRepositorio {

    BloqueioAgenda salvar(BloqueioAgenda bloqueio);

    /** Remove o bloqueio; devolve {@code false} se ele não existia. */
    boolean remover(long id);

    /** Bloqueios que se sobrepõem ao intervalo [inicio, fim). */
    List<BloqueioAgenda> listarEntre(LocalDateTime inicio, LocalDateTime fim);
}
