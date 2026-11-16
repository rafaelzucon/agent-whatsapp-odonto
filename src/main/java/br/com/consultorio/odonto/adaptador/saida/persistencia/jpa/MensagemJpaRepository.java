package br.com.consultorio.odonto.adaptador.saida.persistencia.jpa;

import br.com.consultorio.odonto.adaptador.saida.persistencia.entidade.MensagemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface MensagemJpaRepository extends JpaRepository<MensagemEntity, Long> {

    boolean existsByWaMessageId(String waMessageId);

    @Transactional
    @Modifying
    @Query("delete from MensagemEntity m where m.criadaEm < :limite")
    int removerAnterioresA(@Param("limite") Instant limite);
}
