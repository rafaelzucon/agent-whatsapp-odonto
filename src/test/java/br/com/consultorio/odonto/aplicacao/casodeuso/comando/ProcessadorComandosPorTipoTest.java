package br.com.consultorio.odonto.aplicacao.casodeuso.comando;

import br.com.consultorio.odonto.dominio.modelo.AcaoConversa;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa.BuscarPrimeiroHorarioLivre;
import br.com.consultorio.odonto.dominio.modelo.Conversa;
import br.com.consultorio.odonto.dominio.modelo.WaId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessadorComandosPorTipoTest {

    private static final Conversa CONVERSA = Conversa.nova(new WaId("5511999990001"), Instant.EPOCH);

    @Test
    @DisplayName("Despacha cada comando ao executor registrado para o seu tipo")
    void despachaPorTipo() {
        List<Class<?>> executados = new ArrayList<>();
        ProcessadorComandosPorTipo processador = new ProcessadorComandosPorTipo(todosOsExecutores(executados));

        processador.executar(CONVERSA, new BuscarPrimeiroHorarioLivre());

        assertThat(executados).containsExactly(BuscarPrimeiroHorarioLivre.class);
    }

    @Test
    @DisplayName("Falha na montagem se algum comando da conversa ficar sem executor")
    void exigeExecutorParaTodoComando() {
        List<ExecutorComando<?>> executores = new ArrayList<>(todosOsExecutores(new ArrayList<>()));
        executores.removeIf(e -> e.tipo() == BuscarPrimeiroHorarioLivre.class);

        assertThatThrownBy(() -> new ProcessadorComandosPorTipo(executores))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BuscarPrimeiroHorarioLivre");
    }

    @Test
    @DisplayName("Falha na montagem se um comando tiver dois executores")
    void recusaExecutorDuplicado() {
        List<ExecutorComando<?>> executores = new ArrayList<>(todosOsExecutores(new ArrayList<>()));
        executores.add(registrando(BuscarPrimeiroHorarioLivre.class, new ArrayList<>()));

        assertThatThrownBy(() -> new ProcessadorComandosPorTipo(executores))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mais de um executor");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<ExecutorComando<?>> todosOsExecutores(List<Class<?>> executados) {
        return Arrays.stream(ComandoConversa.class.getPermittedSubclasses())
                .<ExecutorComando<?>>map(tipo -> registrando((Class) tipo, executados))
                .toList();
    }

    private static <C extends ComandoConversa> ExecutorComando<C> registrando(Class<C> tipo,
                                                                             List<Class<?>> executados) {
        return new ExecutorComando<>() {
            @Override
            public Class<C> tipo() {
                return tipo;
            }

            @Override
            public AcaoConversa executar(Conversa conversa, C comando) {
                executados.add(tipo);
                return AcaoConversa.silencio();
            }
        };
    }
}
