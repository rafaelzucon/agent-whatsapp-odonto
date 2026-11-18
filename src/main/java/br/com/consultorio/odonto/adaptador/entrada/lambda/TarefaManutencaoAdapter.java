package br.com.consultorio.odonto.adaptador.entrada.lambda;

import br.com.consultorio.odonto.aplicacao.porta.entrada.ExpirarConversasUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ExpurgarMensagensUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ExpurgarMensagensUseCase.ResultadoExpurgo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Equivalente serverless das {@code TarefasAgendadas}: executa a tarefa de manutenção pedida por um agendador
 * externo (no AWS, o EventBridge Scheduler).
 */
public class TarefaManutencaoAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(TarefaManutencaoAdapter.class);

    public enum Tarefa {
        EXPIRAR_CONVERSAS("expirar-conversas"),
        EXPURGAR_MENSAGENS("expurgar-mensagens");

        private final String nome;

        Tarefa(String nome) {
            this.nome = nome;
        }

        public String nome() {
            return nome;
        }

        public static Tarefa porNome(String nome) {
            return Arrays.stream(values()).filter(t -> t.nome.equals(nome)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Tarefa de manutenção desconhecida: " + nome
                            + ". Use expirar-conversas ou expurgar-mensagens."));
        }
    }

    private final ExpirarConversasUseCase expirarConversas;
    private final ExpurgarMensagensUseCase expurgarMensagens;

    public TarefaManutencaoAdapter(ExpirarConversasUseCase expirarConversas, ExpurgarMensagensUseCase expurgarMensagens) {
        this.expirarConversas = Objects.requireNonNull(expirarConversas);
        this.expurgarMensagens = Objects.requireNonNull(expurgarMensagens);
    }

    /** @return resumo do que foi feito (vai para o log e para o resultado da invocação) */
    public Map<String, Object> executar(String nomeTarefa) {
        Tarefa tarefa = Tarefa.porNome(nomeTarefa);
        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("tarefa", tarefa.nome());
        switch (tarefa) {
            case EXPIRAR_CONVERSAS -> resumo.put("conversasExpiradas", expirarConversas.expirar());
            case EXPURGAR_MENSAGENS -> {
                ResultadoExpurgo resultado = expurgarMensagens.expurgar();
                resumo.put("mensagensExpurgadas", resultado.mensagens());
                resumo.put("conversasExpurgadas", resultado.conversas());
            }
        }
        LOG.info("Manutenção concluída: {}", resumo);
        return resumo;
    }
}
