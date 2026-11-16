package br.com.consultorio.odonto.adaptador.saida.persistencia.jpa;

import br.com.consultorio.odonto.adaptador.saida.persistencia.entidade.ProntuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProntuarioJpaRepository extends JpaRepository<ProntuarioEntity, Long> {

    Optional<ProntuarioEntity> findByPacienteId(Long pacienteId);

    @Query("select p from ProntuarioEntity p join p.queixas q where q.id = :queixaId")
    Optional<ProntuarioEntity> findByQueixaId(@Param("queixaId") Long queixaId);

    @Query(value = "select nextval('prontuario_numero_seq')", nativeQuery = true)
    long proximoNumero();
}
