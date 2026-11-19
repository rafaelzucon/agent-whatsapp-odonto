package br.com.consultorio.odonto.adaptador.saida.persistencia;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Liga as entidades e os repositórios Spring Data JPA só quando a persistência escolhida é o PostgreSQL. */
@Configuration(proxyBeanMethods = false)
@PersistenciaJpa
@EntityScan("br.com.consultorio.odonto.adaptador.saida.persistencia.entidade")
@EnableJpaRepositories("br.com.consultorio.odonto.adaptador.saida.persistencia.jpa")
public class PersistenciaJpaConfig {
}
