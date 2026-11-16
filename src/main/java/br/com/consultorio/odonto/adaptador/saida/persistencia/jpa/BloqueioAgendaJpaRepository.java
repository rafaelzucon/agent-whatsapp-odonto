package br.com.consultorio.odonto.adaptador.saida.persistencia.jpa;

import br.com.consultorio.odonto.adaptador.saida.persistencia.entidade.BloqueioAgendaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface BloqueioAgendaJpaRepository extends JpaRepository<BloqueioAgendaEntity, Long> {

    /** Bloqueios que se sobrepõem ao intervalo [inicio, fim). */
    List<BloqueioAgendaEntity> findByInicioLessThanAndFimGreaterThanOrderByInicio(Instant fim, Instant inicio);
}
