package br.com.consultorio.odonto.infraestrutura.lambda;

import br.com.consultorio.odonto.adaptador.entrada.lambda.ConsumidorFilaAdapter;
import br.com.consultorio.odonto.adaptador.entrada.lambda.TarefaManutencaoAdapter;
import br.com.consultorio.odonto.adaptador.saida.persistencia.catalogo.CatalogoProblemasClasspathAdapter;
import br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb.GerenciadorTransacaoDynamoDb;
import br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb.PacienteDynamoDbAdapter;
import br.com.consultorio.odonto.aplicacao.porta.saida.CatalogoProblemasRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.PacienteRepositorio;
import br.com.consultorio.odonto.infraestrutura.AgenteOdontoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O contexto do perfil "lambda" precisa subir sem chamar a AWS: a inicialização entra no snapshot do SnapStart.
 * O DynamoDB aqui aponta para uma porta fechada de propósito, e não há JDBC/Hibernate/Flyway no contexto.
 */
class PerfilLambdaTest {

    @Test
    void sobeComDynamoDbSemJpaComExecutorRastreadoESemTarefasAgendadas() {
        try (ConfigurableApplicationContext contexto = new SpringApplicationBuilder(AgenteOdontoApplication.class)
                .web(WebApplicationType.NONE)
                .profiles(ContextoSpringLambda.PERFIL)
                .properties("DYNAMODB_TABELA=teste", "DYNAMODB_ENDPOINT=http://127.0.0.1:9", "AWS_REGION=sa-east-1",
                        "CRIPTO_CHAVE_DADOS=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                        "CRIPTO_CHAVE_HASH=ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=",
                        "ANTHROPIC_API_KEY=teste", "OPENAI_API_KEY=teste")
                .run()) {

            assertThat(contexto.getBean(ConsumidorFilaAdapter.class)).isNotNull();
            assertThat(contexto.getBean(PacienteRepositorio.class)).isInstanceOf(PacienteDynamoDbAdapter.class);
            assertThat(contexto.getBean(CatalogoProblemasRepositorio.class))
                    .isInstanceOf(CatalogoProblemasClasspathAdapter.class);
            assertThat(contexto.getBean(PlatformTransactionManager.class))
                    .isInstanceOf(GerenciadorTransacaoDynamoDb.class);
            assertThat(contexto.getBeanNamesForType(DataSource.class)).isEmpty();
            assertThat(contexto.getBean(TarefaManutencaoAdapter.class)).isNotNull();
            assertThat(contexto.getBean(AsyncConfigurer.class).getAsyncExecutor())
                    .isSameAs(contexto.getBean(ExecutorAssincronoRastreado.class));
            assertThat(contexto.getBeansOfType(ScheduledTaskHolder.class).values())
                    .allSatisfy(agendador -> assertThat(agendador.getScheduledTasks()).isEmpty());
        }
    }
}
