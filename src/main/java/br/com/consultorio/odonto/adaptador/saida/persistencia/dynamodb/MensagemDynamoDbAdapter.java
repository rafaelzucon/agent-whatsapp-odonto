package br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb;

import br.com.consultorio.odonto.adaptador.saida.persistencia.PersistenciaDynamoDb;
import br.com.consultorio.odonto.adaptador.saida.persistencia.cripto.CriptografiaService;
import br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb.Atributos.Item;
import br.com.consultorio.odonto.aplicacao.excecao.MensagemDuplicadaException;
import br.com.consultorio.odonto.aplicacao.porta.saida.MensagemRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.RetencaoMensagens;
import br.com.consultorio.odonto.dominio.modelo.DirecaoMensagem;
import br.com.consultorio.odonto.dominio.modelo.Mensagem;
import br.com.consultorio.odonto.dominio.modelo.TipoConteudo;
import br.com.consultorio.odonto.dominio.modelo.WaId;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Histórico de mensagens no DynamoDB. O item-trava {@code WAMSG#<id da Meta>}, gravado com condição na mesma
 * transação, garante a idempotência do webhook (o papel da coluna única {@code wa_message_id} do PostgreSQL).
 */
@Repository
@PersistenciaDynamoDb
public class MensagemDynamoDbAdapter implements MensagemRepositorio, RetencaoMensagens {

    private static final String TIPO = "MENSAGEM";

    private final TabelaDynamoDb tabela;
    private final CriptografiaService cripto;

    public MensagemDynamoDbAdapter(TabelaDynamoDb tabela, CriptografiaService cripto) {
        this.tabela = tabela;
        this.cripto = cripto;
    }

    private static Chave chave(long id) {
        return new Chave(TIPO + "#" + id, TIPO);
    }

    private static Chave chaveWaMessageId(String waMessageId) {
        return new Chave("WAMSG#" + waMessageId, "WAMSG");
    }

    @Override
    public Mensagem salvar(Mensagem mensagem) {
        long id = mensagem.id().orElseGet(() -> tabela.proximoValor("mensagem"));
        mensagem.waMessageId().ifPresent(waMessageId -> tabela.gravar(
                new Item().chave(chaveWaMessageId(waMessageId)).com("mensagemId", id).mapa(),
                Condicao.de("attribute_not_exists(pk) OR mensagemId = :id",
                        Map.of(":id", AttributeValue.fromN(Long.toString(id))),
                        () -> new MensagemDuplicadaException("Mensagem " + waMessageId + " já recebida."))));
        Map<String, AttributeValue> item = new Item()
                .chave(chave(id))
                .indice(TabelaDynamoDb.Indice.GSI1, TIPO,
                        Atributos.ordenavel(mensagem.criadaEm()) + "#" + Atributos.ordenavel(id))
                .com("id", id)
                .com("waMessageId", mensagem.waMessageId().orElse(null))
                .com("waId", mensagem.waId().valor())
                .com("direcao", mensagem.direcao())
                .com("tipo", mensagem.tipo())
                .com("texto", cripto.cifrar(mensagem.texto().orElse(null)))
                .com("midiaId", mensagem.midiaId().orElse(null))
                .com("automatica", mensagem.automatica())
                .com("criadaEm", mensagem.criadaEm())
                .mapa();
        tabela.gravar(item);
        return paraDominio(item);
    }

    @Override
    public boolean existePorWaMessageId(String waMessageId) {
        return tabela.buscar(chaveWaMessageId(waMessageId)).isPresent();
    }

    @Override
    public Optional<Mensagem> buscarPorId(long id) {
        return tabela.buscar(chave(id)).map(this::paraDominio);
    }

    /** Remove na hora as mensagens (e suas travas de idempotência) criadas antes do limite. */
    @Override
    public int removerAnterioresA(Instant limite) {
        int removidas = 0;
        for (Map<String, AttributeValue> item : tabela.consultar(TabelaDynamoDb.Indice.GSI1, TIPO, null,
                Atributos.ordenavel(limite))) {
            String waMessageId = Atributos.texto(item, "waMessageId");
            if (waMessageId != null) {
                tabela.removerAgora(chaveWaMessageId(waMessageId), null, Map.of());
            }
            if (tabela.removerAgora(Chave.de(item), null, Map.of())) {
                removidas++;
            }
        }
        return removidas;
    }

    private Mensagem paraDominio(Map<String, AttributeValue> item) {
        return Mensagem.restaurar(Atributos.numero(item, "id"), Atributos.texto(item, "waMessageId"),
                new WaId(Atributos.texto(item, "waId")), DirecaoMensagem.valueOf(Atributos.texto(item, "direcao")),
                TipoConteudo.valueOf(Atributos.texto(item, "tipo")), cripto.decifrar(Atributos.texto(item, "texto")),
                Atributos.texto(item, "midiaId"), Atributos.logico(item, "automatica"),
                Atributos.instante(item, "criadaEm"));
    }
}
