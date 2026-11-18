package br.com.consultorio.odonto.infraestrutura.lambda;

import br.com.consultorio.odonto.adaptador.entrada.lambda.TarefaManutencaoAdapter;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.springframework.context.ApplicationContext;

import java.util.Map;

/**
 * Função de manutenção, disparada pelo EventBridge Scheduler com {@code {"tarefa": "expirar-conversas"}} ou
 * {@code {"tarefa": "expurgar-mensagens"}} (substitui as tarefas {@code @Scheduled} do modo servidor).
 */
public class ManutencaoHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final TarefaManutencaoAdapter tarefas;
    private final ExecutorAssincronoRastreado executor;

    public ManutencaoHandler() {
        this(ContextoSpringLambda.obter());
    }

    ManutencaoHandler(ApplicationContext contexto) {
        this(contexto.getBean(TarefaManutencaoAdapter.class), contexto.getBean(ExecutorAssincronoRastreado.class));
    }

    ManutencaoHandler(TarefaManutencaoAdapter tarefas, ExecutorAssincronoRastreado executor) {
        this.tarefas = tarefas;
        this.executor = executor;
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> evento, Context contexto) {
        Object tarefa = evento == null ? null : evento.get("tarefa");
        Map<String, Object> resumo = tarefas.executar(tarefa == null ? "" : tarefa.toString());
        AguardaTarefasAssincronas.aguardar(executor, contexto);
        return resumo;
    }
}
