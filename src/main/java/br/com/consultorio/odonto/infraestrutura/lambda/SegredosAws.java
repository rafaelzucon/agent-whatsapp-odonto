package br.com.consultorio.odonto.infraestrutura.lambda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.Parameter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Lê os segredos do AWS Systems Manager Parameter Store (parâmetros padrão do tipo SecureString, sem custo, cifrados
 * com a chave gerenciada {@code aws/ssm}). Cada parâmetro fica em {@code <caminho>/<NOME>}, onde NOME é o MESMO nome
 * da variável de ambiente do modo servidor (WHATSAPP_TOKEN_ACESSO, ANTHROPIC_API_KEY, CRIPTO_CHAVE_DADOS...), então
 * os marcadores {@code ${VAR:}} do application.yml continuam funcionando sem nenhuma mudança.
 *
 * <p>Os valores são lidos uma vez, na inicialização da função. Com SnapStart eles ficam no snapshot (cifrado
 * pela AWS): depois de trocar um segredo, publique uma nova versão da função (novo {@code sam deploy}).
 */
public final class SegredosAws {

    /** Variável de ambiente com o caminho dos parâmetros (ex.: /odonto/segredos). Sem ela, nada é lido. */
    public static final String VARIAVEL_CAMINHO = "ODONTO_PARAMETROS_CAMINHO";

    private static final Logger LOG = LoggerFactory.getLogger(SegredosAws.class);

    private SegredosAws() {
    }

    /** Lê os parâmetros sob o caminho indicado em {@value #VARIAVEL_CAMINHO}; vazio se a variável não existir. */
    public static Map<String, String> ler() {
        String caminho = System.getenv(VARIAVEL_CAMINHO);
        if (caminho == null || caminho.isBlank()) {
            LOG.info("{} não definido: segredos virão apenas do ambiente.", VARIAVEL_CAMINHO);
            return Map.of();
        }
        try (SsmClient cliente = SsmClient.builder().httpClientBuilder(UrlConnectionHttpClient.builder()).build()) {
            Map<String, String> segredos = new LinkedHashMap<>();
            cliente.getParametersByPathPaginator(r -> r.path(caminho).withDecryption(true).recursive(false))
                    .stream().flatMap(pagina -> pagina.parameters().stream())
                    .forEach(p -> segredos.put(nomeCurto(p), p.value()));
            LOG.info("{} segredo(s) carregado(s) do Parameter Store.", segredos.size());
            return segredos;
        }
    }

    /** Último segmento do nome do parâmetro: {@code /odonto/segredos/CRIPTO_CHAVE_DADOS} vira {@code CRIPTO_CHAVE_DADOS}. */
    static String nomeCurto(Parameter parametro) {
        String nome = parametro.name();
        return nome.substring(nome.lastIndexOf('/') + 1);
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
