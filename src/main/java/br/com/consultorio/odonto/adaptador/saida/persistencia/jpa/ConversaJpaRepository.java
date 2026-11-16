package br.com.consultorio.odonto.adaptador.saida.persistencia.jpa;

import br.com.consultorio.odonto.adaptador.saida.persistencia.entidade.ConversaEntity;
import br.com.consultorio.odonto.dominio.modelo.EtapaConversa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface ConversaJpaRepository extends JpaRepository<ConversaEntity, Long> {

    Optional<ConversaEntity> findByWaId(String waId);

    @Transactional
    @Modifying
    @Query("delete from ConversaEntity c where c.ultimaInteracao < :limite and c.etapa in :etapas")
    int removerInativas(@Param("limite") Instant limite, @Param("etapas") Collection<EtapaConversa> etapas);
}
