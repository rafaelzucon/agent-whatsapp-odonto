package br.com.consultorio.odonto.infraestrutura.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Liga as tarefas {@code @Scheduled} só no modo servidor. No AWS Lambda não há processo vivo entre invocações:
 * as mesmas tarefas são disparadas pelo EventBridge Scheduler na função de manutenção.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "odonto.execucao", havingValue = "servidor", matchIfMissing = true)
@EnableScheduling
public class ExecucaoServidorConfig {
}
