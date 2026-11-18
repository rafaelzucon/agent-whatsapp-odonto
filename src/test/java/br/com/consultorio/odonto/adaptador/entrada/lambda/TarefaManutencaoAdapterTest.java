package br.com.consultorio.odonto.adaptador.entrada.lambda;

import br.com.consultorio.odonto.aplicacao.porta.entrada.ExpurgarMensagensUseCase.ResultadoExpurgo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TarefaManutencaoAdapterTest {

    private final TarefaManutencaoAdapter tarefas = new TarefaManutencaoAdapter(() -> 3,
            () -> new ResultadoExpurgo(10, 2));

    @Test
    void executaATarefaPedidaEDevolveOResumo() {
        assertThat(tarefas.executar("expirar-conversas"))
                .containsExactly(entry("tarefa", "expirar-conversas"),
                        entry("conversasExpiradas", 3));
        assertThat(tarefas.executar("expurgar-mensagens"))
                .containsEntry("mensagensExpurgadas", 10).containsEntry("conversasExpurgadas", 2);
    }

    @Test
    void recusaTarefaDesconhecida() {
        assertThatThrownBy(() -> tarefas.executar("apagar-tudo")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expirar-conversas");
    }
}
