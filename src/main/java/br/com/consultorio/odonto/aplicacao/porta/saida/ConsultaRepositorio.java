package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.dominio.excecao.HorarioIndisponivelException;
import br.com.consultorio.odonto.dominio.modelo.Consulta;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Persistência do agregado {@link Consulta}. Datas no fuso do consultório. Nunca devolve {@code null}. */
public interface ConsultaRepositorio {

    /**
     * Grava a consulta e devolve a versão gravada, com id.
     *
     * @throws HorarioIndisponivelException com motivo OCUPADO se outra consulta AGENDADA já começa no
     *                                      mesmo instante (índice único do banco; concorrência)
     */
    Consulta salvar(Consulta consulta);

    Optional<Consulta> buscarPorId(long id);

    /** Consultas AGENDADAS que começam no intervalo [inicio, fim). */
    List<Consulta> listarAgendadasEntre(LocalDateTime inicio, LocalDateTime fim);

    /** Consultas de qualquer status que começam no intervalo [inicio, fim), em ordem de início. */
    List<Consulta> listarEntre(LocalDateTime inicio, LocalDateTime fim);

    /** Consultas do paciente, em ordem de início. */
    List<Consulta> listarPorPaciente(long pacienteId);

    /** Próxima consulta AGENDADA do paciente que começa depois de {@code agora}. */
    Optional<Consulta> proximaAgendadaDoPaciente(long pacienteId, LocalDateTime agora);
}
