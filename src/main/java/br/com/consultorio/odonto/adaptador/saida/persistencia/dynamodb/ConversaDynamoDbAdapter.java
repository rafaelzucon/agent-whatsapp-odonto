package br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb;

import br.com.consultorio.odonto.adaptador.saida.persistencia.DadosConversaJson;
import br.com.consultorio.odonto.adaptador.saida.persistencia.PersistenciaDynamoDb;
import br.com.consultorio.odonto.adaptador.saida.persistencia.cripto.CriptografiaService;
import br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb.Atributos.Item;
import br.com.consultorio.odonto.aplicacao.porta.saida.ConversaRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.RetencaoConversas;
import br.com.consultorio.odonto.dominio.modelo.Conversa;
import br.com.consultorio.odonto.dominio.modelo.EtapaConversa;
import br.com.consultorio.odonto.dominio.modelo.WaId;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Estado das conversas no DynamoDB, uma por número de WhatsApp. Os dados coletados vão em JSON cifrado; o índice
 * GSI1 ordena as conversas pela última interação, para a expiração e o expurgo.
 */
@Repository
@PersistenciaDynamoDb
public class ConversaDynamoDbAdapter implements ConversaRepositorio, RetencaoConversas {

    private static final String TIPO = "CONVERSA";

    private final TabelaDynamoDb tabela;
    private final CriptografiaService cripto;
    private final JsonMapper json;

    public ConversaDynamoDbAdapter(TabelaDynamoDb tabela, CriptografiaService cripto, JsonMapper json) {
        this.tabela = tabela;
        this.cripto = cripto;
        this.json = json;
    }

    private static Chave chave(String waId) {
        return new Chave(TIPO + "#" + waId, TIPO);
    }

    @Override
    public Optional<Conversa> buscarPorWaId(WaId waId) {
        return tabela.buscar(chave(waId.valor())).map(this::paraDominio);
    }

    @Override
    public Conversa salvar(Conversa conversa) {
        String waId = conversa.waId().valor();
        // Uma conversa "nova" pode substituir outra ainda gravada para o mesmo número: reaproveita o id.
        long id = tabela.buscar(chave(waId)).map(existente -> Atributos.numero(existente, "id"))
                .orElseGet(() -> tabela.proximoValor("conversa"));
        Map<String, AttributeValue> item = new Item()
                .chave(chave(waId))
                .indice(TabelaDynamoDb.Indice.GSI1, TIPO, Atributos.ordenavel(conversa.ultimaInteracao()) + "#" + waId)
                .com("id", id)
                .com("waId", waId)
                .com("etapa", conversa.etapa())
                .com("dados", cripto.cifrar(json.writeValueAsString(DadosConversaJson.de(conversa.dados()))))
                .com("tentativasInvalidas", (long) conversa.tentativasInvalidas())
                .com("ultimaInteracao", conversa.ultimaInteracao())
                .com("iniciadaEm", conversa.iniciadaEm())
                .mapa();
        tabela.gravar(item);
        return paraDominio(item);
    }

    /** Remove na hora; a condição evita apagar uma conversa que recebeu mensagem depois da consulta ao índice. */
    @Override
    public int removerInativasDesde(Instant limite, Set<EtapaConversa> etapas) {
        if (etapas.isEmpty()) {
            return 0;
        }
        int removidas = 0;
        for (Map<String, AttributeValue> item : tabela.consultar(TabelaDynamoDb.Indice.GSI1, TIPO, null,
                Atributos.ordenavel(limite))) {
            String etapa = Atributos.texto(item, "etapa");
            if (etapas.stream().anyMatch(e -> e.name().equals(etapa)) && tabela.removerAgora(Chave.de(item),
                    "ultimaInteracao = :u AND etapa = :e",
                    Map.of(":u", item.get("ultimaInteracao"), ":e", item.get("etapa")))) {
                removidas++;
            }
        }
        return removidas;
    }

    private Conversa paraDominio(Map<String, AttributeValue> item) {
        DadosConversaJson dados = json.readValue(cripto.decifrar(Atributos.texto(item, "dados")),
                DadosConversaJson.class);
        return Conversa.restaurar(Atributos.numero(item, "id"), new WaId(Atributos.texto(item, "waId")),
                EtapaConversa.valueOf(Atributos.texto(item, "etapa")), dados.paraDominio(),
                Atributos.numero(item, "tentativasInvalidas").intValue(), Atributos.instante(item, "ultimaInteracao"),
                Atributos.instante(item, "iniciadaEm"));
    }
}
