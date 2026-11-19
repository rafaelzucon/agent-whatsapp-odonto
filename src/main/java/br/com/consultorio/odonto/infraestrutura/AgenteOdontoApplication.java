package br.com.consultorio.odonto.infraestrutura;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Ponto de entrada. Fica na camada de infraestrutura (a mais externa) e varre o projeto inteiro: os
 * adaptadores são componentes Spring; domínio e aplicação são montados à mão em {@code CasosDeUsoConfig}.
 *
 * <p>Roda em dois modos (propriedade {@code odonto.execucao}): {@code servidor} (padrão; Tomcat e tarefas
 * {@code @Scheduled}) e {@code lambda} (perfil Spring "lambda", iniciado pelos handlers de
 * {@code infraestrutura.lambda}). A persistência também é escolhida por propriedade ({@code odonto.persistencia}):
 * {@code jpa} (PostgreSQL, padrão) ou {@code dynamodb} (usado no Lambda, sem servidor de banco).
 */
@SpringBootApplication(scanBasePackages = "br.com.consultorio.odonto")
@ConfigurationPropertiesScan("br.com.consultorio.odonto.infraestrutura")
@EnableAsync
public class AgenteOdontoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgenteOdontoApplication.class, args);
    }
}
