package br.com.consultorio.odonto.adaptador.saida.persistencia.jpa;

import br.com.consultorio.odonto.adaptador.saida.persistencia.entidade.ProblemaOdontologicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemaOdontologicoJpaRepository extends JpaRepository<ProblemaOdontologicoEntity, String> {

    List<ProblemaOdontologicoEntity> findByAtivoTrueOrderByCodigo();
}
