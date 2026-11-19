package br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb;

import br.com.consultorio.odonto.adaptador.saida.persistencia.PersistenciaDynamoDb;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;

/**
 * Persistência no DynamoDB ({@code odonto.persistencia=dynamodb}, ligada pelo perfil "lambda"). O cliente usa o
 * HTTP do próprio JDK (sem Netty/Apache), resolve as credenciais só na primeira chamada e por isso pode ser criado
 * na inicialização capturada pelo SnapStart.
 */
@Configuration(proxyBeanMethods = false)
@PersistenciaDynamoDb
public class PersistenciaDynamoDbConfig {

    @Bean(destroyMethod = "close")
    DynamoDbClient clienteDynamoDb(@Value("${odonto.dynamodb.regiao:}") String regiao,
                                   @Value("${odonto.dynamodb.endpoint:}") String endpoint) {
        DynamoDbClientBuilder construtor = DynamoDbClient.builder().httpClientBuilder(UrlConnectionHttpClient.builder());
        if (!regiao.isBlank()) {
            construtor.region(Region.of(regiao));
        }
        if (!endpoint.isBlank()) {
            construtor.endpointOverride(URI.create(endpoint));
        }
        return construtor.build();
    }

    @Bean
    TabelaDynamoDb tabelaDynamoDb(DynamoDbClient cliente, @Value("${odonto.dynamodb.tabela:}") String tabela) {
        return new TabelaDynamoDb(cliente, tabela);
    }

    @Bean
    PlatformTransactionManager transactionManager(TabelaDynamoDb tabela) {
        return new GerenciadorTransacaoDynamoDb(tabela);
    }
}
