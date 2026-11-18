package br.com.consultorio.odonto.infraestrutura.lambda;

import br.com.consultorio.odonto.adaptador.entrada.lambda.ConsumidorFilaAdapter;
import br.com.consultorio.odonto.adaptador.entrada.lambda.TarefaManutencaoAdapter;
import br.com.consultorio.odonto.infraestrutura.AgenteOdontoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.config.ScheduledTaskHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O contexto do perfil "lambda" precisa subir SEM banco: a inicialização entra no snapshot do SnapStart e não
 * pode abrir conexões (o banco aqui aponta para uma porta fechada de propósito).
 */
class PerfilLambdaTest {

    @Test
    void sobeSemBancoComExecutorRastreadoESemTarefasAgendadas() {
        try (ConfigurableApplicationContext contexto = new SpringApplicationBuilder(AgenteOdontoApplication.class)
                .web(WebApplicationType.NONE)
                .profiles(ContextoSpringLambda.PERFIL)
                .properties("DB_URL=jdbc:postgresql://127.0.0.1:9/inexistente?connectTimeout=1",
                        "CRIPTO_CHAVE_DADOS=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                        "CRIPTO_CHAVE_HASH=ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=",
                        "ANTHROPIC_API_KEY=teste", "OPENAI_API_KEY=teste")
                .run()) {

            assertThat(contexto.getBean(ConsumidorFilaAdapter.class)).isNotNull();
            assertThat(contexto.getBean(TarefaManutencaoAdapter.class)).isNotNull();
            assertThat(contexto.getBean(AsyncConfigurer.class).getAsyncExecutor())
                    .isSameAs(contexto.getBean(ExecutorAssincronoRastreado.class));
            assertThat(contexto.getBeansOfType(ScheduledTaskHolder.class).values())
                    .allSatisfy(agendador -> assertThat(agendador.getScheduledTasks()).isEmpty());
        }
    }
}
