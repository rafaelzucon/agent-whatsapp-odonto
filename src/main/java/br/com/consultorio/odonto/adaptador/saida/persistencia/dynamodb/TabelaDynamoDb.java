package br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb;

import org.springframework.transaction.support.TransactionSynchronizationManager;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Acesso à tabela única do DynamoDB usada pelos repositórios do modo serverless.
 *
 * <p>Dentro de uma transação do {@link GerenciadorTransacaoDynamoDb} as escritas são acumuladas e enviadas juntas
 * no commit; fora dela, são gravadas na hora. As leituras por chave enxergam as escritas pendentes da transação.
 * Consultas aos índices secundários (GSI) são eventualmente consistentes; a unicidade nunca depende delas, e sim
 * de itens-trava gravados com condição.
 */
public class TabelaDynamoDb {

    public static final String PK = "pk";
    public static final String SK = "sk";
    private static final String CONDICAO_FALHOU = "ConditionalCheckFailed";

    private final DynamoDbClient cliente;
    private final String nome;

    public TabelaDynamoDb(DynamoDbClient cliente, String nome) {
        this.cliente = Objects.requireNonNull(cliente);
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da tabela do DynamoDB não configurado (odonto.dynamodb.tabela).");
        }
        this.nome = nome;
    }

    public String nome() {
        return nome;
    }

    public Optional<Map<String, AttributeValue>> buscar(Chave chave) {
        UnidadeDeTrabalho unidade = unidadeAtual();
        if (unidade != null) {
            Optional<Escrita> pendente = unidade.pendente(chave);
            if (pendente.isPresent()) {
                return pendente.get().itemGravado();
            }
        }
        GetItemResponse resposta = cliente.getItem(r -> r.tableName(nome).key(chave.mapa()).consistentRead(true));
        return resposta.hasItem() && !resposta.item().isEmpty() ? Optional.of(resposta.item()) : Optional.empty();
    }

    /**
     * Itens de uma partição do índice, em ordem crescente da chave de ordenação.
     *
     * @param aPartirDe primeiro valor (inclusivo) da chave de ordenação; {@code null} = sem limite inferior
     * @param ate       último valor (inclusivo); {@code null} = sem limite superior. Como as chaves de ordenação
     *                  são {@code <valor>#<id>}, passar só {@code <valor>} exclui os itens com exatamente esse valor
     */
    public List<Map<String, AttributeValue>> consultar(Indice indice, String particao, String aPartirDe, String ate) {
        Map<String, AttributeValue> valores = new HashMap<>();
        valores.put(":p", AttributeValue.fromS(particao));
        StringBuilder expressao = new StringBuilder(indice.particao()).append(" = :p");
        if (aPartirDe != null && ate != null) {
            expressao.append(" AND ").append(indice.ordenacao()).append(" BETWEEN :de AND :ate");
        } else if (aPartirDe != null) {
            expressao.append(" AND ").append(indice.ordenacao()).append(" >= :de");
        } else if (ate != null) {
            expressao.append(" AND ").append(indice.ordenacao()).append(" <= :ate");
        }
        if (aPartirDe != null) {
            valores.put(":de", AttributeValue.fromS(aPartirDe));
        }
        if (ate != null) {
            valores.put(":ate", AttributeValue.fromS(ate));
        }
        QueryRequest consulta = QueryRequest.builder().tableName(nome).indexName(indice.nome())
                .keyConditionExpression(expressao.toString()).expressionAttributeValues(valores).build();
        List<Map<String, AttributeValue>> itens = new ArrayList<>();
        cliente.queryPaginator(consulta).items().forEach(itens::add);
        return itens;
    }

    /** Grava (substitui) o item; com condição, a falha vira a exceção da {@link Condicao}. */
    public void gravar(Map<String, AttributeValue> item, Condicao condicao) {
        registrar(Escrita.gravacao(item, condicao));
    }

    public void gravar(Map<String, AttributeValue> item) {
        gravar(item, null);
    }

    public void remover(Chave chave, Condicao condicao) {
        registrar(Escrita.remocao(chave, condicao));
    }

    public void remover(Chave chave) {
        remover(chave, null);
    }

    /**
     * Remove na hora, mesmo dentro de uma transação (expurgos podem passar do limite de itens de uma transação e
     * são idempotentes). Devolve {@code false} se o item não existia ou se a condição não foi atendida.
     */
    public boolean removerAgora(Chave chave, String condicao, Map<String, AttributeValue> valores) {
        DeleteItemRequest.Builder remocao = DeleteItemRequest.builder().tableName(nome).key(chave.mapa())
                .returnValues(ReturnValue.ALL_OLD);
        if (condicao != null) {
            remocao.conditionExpression(condicao);
            if (!valores.isEmpty()) {
                remocao.expressionAttributeValues(valores);
            }
        }
        try {
            return cliente.deleteItem(remocao.build()).hasAttributes();
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }

    /** Próximo valor de um contador atômico (como uma sequence do PostgreSQL: fora da transação e sem volta). */
    public long proximoValor(String contador) {
        Chave chave = new Chave("CONTADOR#" + contador, "CONTADOR");
        return Long.parseLong(cliente.updateItem(r -> r.tableName(nome).key(chave.mapa())
                .updateExpression("ADD valor :um")
                .expressionAttributeValues(Map.of(":um", AttributeValue.fromN("1")))
                .returnValues(ReturnValue.UPDATED_NEW)).attributes().get("valor").n());
    }

    void confirmar(UnidadeDeTrabalho unidade) {
        List<Escrita> escritas = unidade.escritas();
        if (escritas.size() == 1) {
            executar(escritas.getFirst());
        } else if (escritas.size() > 1) {
            try {
                cliente.transactWriteItems(r -> r.transactItems(
                        escritas.stream().map(e -> e.paraTransacao(nome)).toList()));
            } catch (TransactionCanceledException e) {
                throw traduzir(e, escritas);
            }
        }
        unidade.descartar();
    }

    private void registrar(Escrita escrita) {
        UnidadeDeTrabalho unidade = unidadeAtual();
        if (unidade != null) {
            unidade.registrar(escrita);
        } else {
            executar(escrita);
        }
    }

    private void executar(Escrita escrita) {
        Condicao condicao = escrita.condicao();
        try {
            if (escrita.item() != null) {
                PutItemRequest.Builder put = PutItemRequest.builder().tableName(nome).item(escrita.item());
                if (condicao != null) {
                    put.conditionExpression(condicao.expressao());
                    if (!condicao.valores().isEmpty()) {
                        put.expressionAttributeValues(condicao.valores());
                    }
                }
                cliente.putItem(put.build());
            } else {
                DeleteItemRequest.Builder delete = DeleteItemRequest.builder().tableName(nome)
                        .key(escrita.chave().mapa());
                if (condicao != null) {
                    delete.conditionExpression(condicao.expressao());
                    if (!condicao.valores().isEmpty()) {
                        delete.expressionAttributeValues(condicao.valores());
                    }
                }
                cliente.deleteItem(delete.build());
            }
        } catch (ConditionalCheckFailedException e) {
            if (condicao == null) {
                throw e;
            }
            throw condicao.violacao().get();
        }
    }

    private static RuntimeException traduzir(TransactionCanceledException e, List<Escrita> escritas) {
        List<CancellationReason> motivos = e.hasCancellationReasons() ? e.cancellationReasons() : List.of();
        for (int i = 0; i < motivos.size() && i < escritas.size(); i++) {
            Condicao condicao = escritas.get(i).condicao();
            if (CONDICAO_FALHOU.equals(motivos.get(i).code()) && condicao != null) {
                return condicao.violacao().get();
            }
        }
        return e;
    }

    private UnidadeDeTrabalho unidadeAtual() {
        return (UnidadeDeTrabalho) TransactionSynchronizationManager.getResource(this);
    }

    /** Índices secundários globais da tabela (iguais aos do aws/template.yaml). */
    public enum Indice {
        /** Listagens ordenadas por tempo ou id de cada tipo de item (consultas, bloqueios, mensagens...). */
        GSI1("gsi1", "gsi1pk", "gsi1sk"),
        /** Consultas de um paciente, ordenadas pelo início. */
        GSI2("gsi2", "gsi2pk", "gsi2sk");

        private final String nome;
        private final String particao;
        private final String ordenacao;

        Indice(String nome, String particao, String ordenacao) {
            this.nome = nome;
            this.particao = particao;
            this.ordenacao = ordenacao;
        }

        public String nome() {
            return nome;
        }

        public String particao() {
            return particao;
        }

        public String ordenacao() {
            return ordenacao;
        }
    }
}
