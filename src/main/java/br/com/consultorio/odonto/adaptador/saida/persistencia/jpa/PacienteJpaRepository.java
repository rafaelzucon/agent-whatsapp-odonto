package br.com.consultorio.odonto.adaptador.saida.persistencia.jpa;

import br.com.consultorio.odonto.adaptador.saida.persistencia.entidade.PacienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PacienteJpaRepository extends JpaRepository<PacienteEntity, Long> {

    Optional<PacienteEntity> findByRgHash(String rgHash);
}
