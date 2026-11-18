package br.com.consultorio.odonto.infraestrutura.lambda;

import br.com.consultorio.odonto.adaptador.entrada.lambda.CodecMensagemFila;
import br.com.consultorio.odonto.adaptador.entrada.lambda.ConsumidorFilaAdapter;
import br.com.consultorio.odonto.adaptador.entrada.lambda.TarefaManutencaoAdapter;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ExpirarConversasUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ExpurgarMensagensUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ReceberMensagensUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.Executor;

/** Beans exclusivos do modo AWS Lambda (perfil "lambda", que define {@code odonto.execucao=lambda}). */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "odonto.execucao", havingValue = "lambda")
public class ExecucaoLambdaConfig {

    @Bean
    ExecutorAssincronoRastreado executorAssincronoRastreado() {
        return new ExecutorAssincronoRastreado();
    }

    @Bean
    AsyncConfigurer configuradorAssincronoLambda(ExecutorAssincronoRastreado executor) {
        return new AsyncConfigurer() {
            @Override
            public Executor getAsyncExecutor() {
                return executor;
            }
        };
    }

    @Bean
    CodecMensagemFila codecMensagemFila() {
        // Mesmo mapeador padrão usado pela função do webhook, que não sobe o Spring.
        return new CodecMensagemFila(JsonMapper.builder().build());
    }

    @Bean
    ConsumidorFilaAdapter consumidorFilaAdapter(CodecMensagemFila codec, ReceberMensagensUseCase receberMensagens) {
        return new ConsumidorFilaAdapter(codec, receberMensagens);
    }

    @Bean
    TarefaManutencaoAdapter tarefaManutencaoAdapter(ExpirarConversasUseCase expirarConversas,
                                                    ExpurgarMensagensUseCase expurgarMensagens) {
        return new TarefaManutencaoAdapter(expirarConversas, expurgarMensagens);
    }
}
