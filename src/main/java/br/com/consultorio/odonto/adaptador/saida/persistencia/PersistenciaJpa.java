package br.com.consultorio.odonto.adaptador.saida.persistencia;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Ativa o componente quando {@code odonto.persistencia=jpa} (padrão: PostgreSQL no modo servidor/Docker). */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(name = "odonto.persistencia", havingValue = "jpa", matchIfMissing = true)
public @interface PersistenciaJpa {
}
