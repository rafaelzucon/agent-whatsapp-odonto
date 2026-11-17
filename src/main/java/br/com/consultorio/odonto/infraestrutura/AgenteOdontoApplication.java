package br.com.consultorio.odonto.infraestrutura;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ponto de entrada. Fica na camada de infraestrutura (a mais externa) e varre o projeto inteiro: os
 * adaptadores são componentes Spring; domínio e aplicação são montados à mão em {@code CasosDeUsoConfig}.
 */
@SpringBootApplication(scanBasePackages = "br.com.consultorio.odonto")
@EntityScan("br.com.consultorio.odonto.adaptador.saida.persistencia.entidade")
@EnableJpaRepositories("br.com.consultorio.odonto.adaptador.saida.persistencia.jpa")
@ConfigurationPropertiesScan("br.com.consultorio.odonto.infraestrutura")
@EnableAsync
@EnableScheduling
public class AgenteOdontoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgenteOdontoApplication.class, args);
    }
}
