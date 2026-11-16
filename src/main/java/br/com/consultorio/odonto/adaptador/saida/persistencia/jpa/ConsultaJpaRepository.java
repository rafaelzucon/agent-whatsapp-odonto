package br.com.consultorio.odonto.adaptador.saida.persistencia.jpa;

import br.com.consultorio.odonto.adaptador.saida.persistencia.entidade.ConsultaEntity;
import br.com.consultorio.odonto.dominio.modelo.StatusConsulta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConsultaJpaRepository extends JpaRepository<ConsultaEntity, Long> {

    List<ConsultaEntity> findByInicioGreaterThanEqualAndInicioLessThanOrderByInicio(Instant inicio, Instant fim);

    List<ConsultaEntity> findByStatusAndInicioGreaterThanEqualAndInicioLessThanOrderByInicio(
            StatusConsulta status, Instant inicio, Instant fim);

    List<ConsultaEntity> findByPacienteIdOrderByInicio(Long pacienteId);

    Optional<ConsultaEntity> findFirstByPacienteIdAndStatusAndInicioGreaterThanOrderByInicio(
            Long pacienteId, StatusConsulta status, Instant agora);
}
