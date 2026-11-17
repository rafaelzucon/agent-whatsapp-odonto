package br.com.consultorio.odonto.infraestrutura.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cliente de chat do Spring AI sobre o modelo configurado ({@code spring.ai.model.chat=anthropic}).
 * Usado só para classificar a queixa e interpretar data e hora; nunca para orientação clínica.
 */
@Configuration(proxyBeanMethods = false)
public class IaConfig {

    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
