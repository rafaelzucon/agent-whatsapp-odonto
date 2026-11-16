package br.com.consultorio.odonto.adaptador.saida.persistencia;

import br.com.consultorio.odonto.adaptador.saida.persistencia.entidade.ConsultaEntity;
import br.com.consultorio.odonto.adaptador.saida.persistencia.jpa.ConsultaJpaRepository;
import br.com.consultorio.odonto.aplicacao.porta.saida.ConsultaRepositorio;
import br.com.consultorio.odonto.dominio.excecao.HorarioIndisponivelException;
import br.com.consultorio.odonto.dominio.excecao.MotivoIndisponibilidade;
import br.com.consultorio.odonto.dominio.modelo.Consulta;
import br.com.consultorio.odonto.dominio.modelo.Horario;
import br.com.consultorio.odonto.dominio.modelo.StatusConsulta;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Consultas da agenda. O índice único parcial {@code uk_consulta_inicio_agendada} impede duas consultas
 * AGENDADAS no mesmo horário mesmo com pedidos simultâneos; a violação vira {@link HorarioIndisponivelException}.
 */
@Repository
@Transactional(readOnly = true)
public class ConsultaJpaAdapter implements ConsultaRepositorio {

    static final String INDICE_HORARIO_UNICO = "uk_consulta_inicio_agendada";

    private final ConsultaJpaRepository repositorio;
    private final FusoPersistencia fuso;

    ConsultaJpaAdapter(ConsultaJpaRepository repositorio, FusoPersistencia fuso) {
        this.repositorio = repositorio;
        this.fuso = fuso;
    }

    @Override
    @Transactional
    public Consulta salvar(Consulta consulta) {
        ConsultaEntity entidade = new ConsultaEntity(consulta.id().orElse(null), consulta.pacienteId(),
                consulta.queixaId().orElse(null), fuso.paraInstante(consulta.horario().inicio()),
                fuso.paraInstante(consulta.horario().fim()), consulta.status(), consulta.criadaEm());
        try {
            return paraDominio(repositorio.saveAndFlush(entidade));
        } catch (DataIntegrityViolationException e) {
            if (ViolacaoRestricao.envolve(e, INDICE_HORARIO_UNICO)) {
                throw new HorarioIndisponivelException(MotivoIndisponibilidade.OCUPADO);
            }
            throw e;
        }
    }

    @Override
    public Optional<Consulta> buscarPorId(long id) {
        return repositorio.findById(id).map(this::paraDominio);
    }

    @Override
    public List<Consulta> listarAgendadasEntre(LocalDateTime inicio, LocalDateTime fim) {
        return repositorio.findByStatusAndInicioGreaterThanEqualAndInicioLessThanOrderByInicio(StatusConsulta.AGENDADA,
                fuso.paraInstante(inicio), fuso.paraInstante(fim)).stream().map(this::paraDominio).toList();
    }

    @Override
    public List<Consulta> listarEntre(LocalDateTime inicio, LocalDateTime fim) {
        return repositorio.findByInicioGreaterThanEqualAndInicioLessThanOrderByInicio(fuso.paraInstante(inicio),
                fuso.paraInstante(fim)).stream().map(this::paraDominio).toList();
    }

    @Override
    public List<Consulta> listarPorPaciente(long pacienteId) {
        return repositorio.findByPacienteIdOrderByInicio(pacienteId).stream().map(this::paraDominio).toList();
    }

    @Override
    public Optional<Consulta> proximaAgendadaDoPaciente(long pacienteId, LocalDateTime agora) {
        return repositorio.findFirstByPacienteIdAndStatusAndInicioGreaterThanOrderByInicio(pacienteId,
                StatusConsulta.AGENDADA, fuso.paraInstante(agora)).map(this::paraDominio);
    }

    private Consulta paraDominio(ConsultaEntity e) {
        return Consulta.restaurar(e.getId(), e.getPacienteId(), e.getQueixaId(),
                new Horario(fuso.paraLocal(e.getInicio()), fuso.paraLocal(e.getFim())), e.getStatus(),
                e.getCriadaEm());
    }
}
