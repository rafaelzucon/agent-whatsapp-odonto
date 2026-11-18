package br.com.consultorio.odonto.infraestrutura.lambda;

import br.com.consultorio.odonto.infraestrutura.AgenteOdontoApplication;
import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.InitializationWrapper;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Função da API administrativa ({@code /api/admin/**}, protegida pelo {@code ADMIN_TOKEN}): roda os mesmos
 * controllers Spring MVC do modo servidor por meio do aws-serverless-java-container, sem abrir porta HTTP.
 * <p>
 * O tipo SERVLET é declarado explicitamente: o atalho {@code getHttpApiV2ProxyHandler} parte do tipo REACTIVE
 * e falharia, porque o WebFlux também está no classpath (trazido pelo Spring AI).
 */
public class AdminHandler implements RequestStreamHandler {

    private final SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> container;
    private final ExecutorAssincronoRastreado executor;

    public AdminHandler() throws ContainerInitializationException {
        ContextoSpringLambda.prepararAmbiente();
        this.container = new SpringBootProxyHandlerBuilder<HttpApiV2ProxyRequest>()
                .defaultHttpApiV2Proxy()
                .initializationWrapper(new InitializationWrapper())
                .servletApplication()
                .springBootApplication(AgenteOdontoApplication.class)
                .profiles(ContextoSpringLambda.PERFIL)
                .buildAndInitialize();
        WebApplicationContext contexto = WebApplicationContextUtils
                .getRequiredWebApplicationContext(container.getServletContext());
        this.executor = contexto.getBean(ExecutorAssincronoRastreado.class);
    }

    @Override
    public void handleRequest(InputStream entrada, OutputStream saida, Context contexto) throws IOException {
        container.proxyStream(entrada, saida, contexto);
        AguardaTarefasAssincronas.aguardar(executor, contexto);
    }
}
