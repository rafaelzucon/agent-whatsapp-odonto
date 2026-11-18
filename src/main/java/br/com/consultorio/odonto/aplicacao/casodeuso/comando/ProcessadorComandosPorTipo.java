package br.com.consultorio.odonto.aplicacao.casodeuso.comando;

import br.com.consultorio.odonto.dominio.modelo.AcaoConversa;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa;
import br.com.consultorio.odonto.dominio.modelo.Conversa;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Despacha cada comando ao {@link ExecutorComando} registrado para o seu tipo. Falha já na montagem se algum
 * comando da conversa ficar sem executor ou tiver dois — o erro aparece na subida, não no meio de uma conversa.
 */
public final class ProcessadorComandosPorTipo implements ProcessadorComandosConversa {

    private final Map<Class<?>, ExecutorComando<?>> executores;

    public ProcessadorComandosPorTipo(List<? extends ExecutorComando<?>> executores) {
        Map<Class<?>, ExecutorComando<?>> porTipo = new HashMap<>();
        for (ExecutorComando<?> executor : executores) {
            if (porTipo.putIfAbsent(executor.tipo(), executor) != null) {
                throw new IllegalArgumentException("Mais de um executor para " + executor.tipo().getSimpleName() + ".");
            }
        }
        List<String> semExecutor = Arrays.stream(ComandoConversa.class.getPermittedSubclasses())
                .filter(tipo -> !porTipo.containsKey(tipo))
                .map(Class::getSimpleName)
                .toList();
        if (!semExecutor.isEmpty()) {
            throw new IllegalArgumentException("Comandos sem executor: " + semExecutor + ".");
        }
        this.executores = Map.copyOf(porTipo);
    }

    @Override
    public AcaoConversa executar(Conversa conversa, ComandoConversa comando) {
        return executarCom(executores.get(comando.getClass()), conversa, comando);
    }

    private static <C extends ComandoConversa> AcaoConversa executarCom(ExecutorComando<C> executor,
                                                                       Conversa conversa, ComandoConversa comando) {
        return executor.executar(conversa, executor.tipo().cast(comando));
    }
}
