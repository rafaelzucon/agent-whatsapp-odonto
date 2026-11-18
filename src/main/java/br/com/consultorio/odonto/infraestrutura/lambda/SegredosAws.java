package br.com.consultorio.odonto.infraestrutura.lambda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Lê os segredos do AWS Secrets Manager. O segredo é um JSON cujas chaves têm os MESMOS nomes das variáveis de
 * ambiente do modo servidor (WHATSAPP_TOKEN_ACESSO, ANTHROPIC_API_KEY, CRIPTO_CHAVE_DADOS, DB_SENHA...), então os
 * marcadores {@code ${VAR:}} do application.yml continuam funcionando sem nenhuma mudança.
 *
 * <p>Os valores são lidos uma vez, na inicialização da função. Com SnapStart eles ficam no snapshot (cifrado
 * pela AWS): depois de trocar um segredo, publique uma nova versão da função (novo {@code sam deploy}).
 */
public final class SegredosAws {

    /** Variável de ambiente com o nome ou ARN do segredo. Sem ela, nada é lido (útil em testes locais). */
    public static final String VARIAVEL_ID_SEGREDO = "ODONTO_SEGREDO_ID";

    private static final Logger LOG = LoggerFactory.getLogger(SegredosAws.class);
    private static final TypeReference<Map<String, Object>> TIPO_MAPA = new TypeReference<>() {
    };

    private SegredosAws() {
    }

    /** Lê o segredo indicado em {@value #VARIAVEL_ID_SEGREDO}; devolve vazio se a variável não existir. */
    public static Map<String, String> ler() {
        String id = System.getenv(VARIAVEL_ID_SEGREDO);
        if (id == null || id.isBlank()) {
            LOG.info("{} não definido: segredos virão apenas do ambiente.", VARIAVEL_ID_SEGREDO);
            return Map.of();
        }
        try (SecretsManagerClient cliente = SecretsManagerClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder()).build()) {
            String json = cliente.getSecretValue(r -> r.secretId(id)).secretString();
            Map<String, String> segredos = interpretar(json);
            LOG.info("{} segredo(s) carregado(s) do Secrets Manager.", segredos.size());
            return segredos;
        }
    }

    static Map<String, String> interpretar(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        Map<String, Object> bruto = JsonMapper.builder().build().readValue(json, TIPO_MAPA);
        Map<String, String> segredos = new LinkedHashMap<>();
        bruto.forEach((chave, valor) -> {
            if (valor != null) {
                segredos.put(chave, String.valueOf(valor));
            }
        });
        return segredos;
    }

    /**
     * Publica os segredos como propriedades de sistema da JVM, que o Spring consulta antes das variáveis de
     * ambiente. Não sobrescreve uma propriedade de sistema já definida (permite ajuste fino via JAVA_TOOL_OPTIONS).
     */
    public static void publicarComoPropriedadesDoSistema(Map<String, String> segredos) {
        segredos.forEach((chave, valor) -> {
            if (System.getProperty(chave) == null) {
                System.setProperty(chave, valor);
            }
        });
    }

    /** Busca um valor primeiro nos segredos e depois nas variáveis de ambiente. */
    public static UnaryOperator<String> resolvedor(Map<String, String> segredos) {
        Objects.requireNonNull(segredos);
        return nome -> {
            String valor = segredos.get(nome);
            return valor != null ? valor : System.getenv(nome);
        };
    }
}
