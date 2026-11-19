package br.com.consultorio.odonto.infraestrutura.lambda;

import br.com.consultorio.odonto.infraestrutura.AgenteOdontoApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Sobe (uma única vez por ambiente de execução) o contexto Spring das funções que usam a aplicação completa.
 * É chamado no construtor dos handlers, ou seja, na fase de inicialização do Lambda: com SnapStart o contexto
 * pronto entra no snapshot e as invocações seguintes não pagam a partida a frio do Spring.
 */
public final class ContextoSpringLambda {

    public static final String PERFIL = "lambda";

    private static ConfigurableApplicationContext contexto;

    private ContextoSpringLambda() {
    }

    public static synchronized ConfigurableApplicationContext obter() {
        if (contexto == null) {
            prepararAmbiente();
            contexto = new SpringApplicationBuilder(AgenteOdontoApplication.class)
                    .web(WebApplicationType.NONE)
                    .profiles(PERFIL)
                    .run();
        }
        return contexto;
    }

    /** Carrega os segredos do Parameter Store como propriedades de sistema (idempotente). */
    public static synchronized void prepararAmbiente() {
        if (System.getProperty("odonto.lambda.segredos-carregados") == null) {
            SegredosAws.publicarComoPropriedadesDoSistema(SegredosAws.ler());
            System.setProperty("odonto.lambda.segredos-carregados", "true");
        }
    }
}
