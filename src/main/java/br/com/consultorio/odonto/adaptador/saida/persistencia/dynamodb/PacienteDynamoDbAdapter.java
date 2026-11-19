package br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb;

import br.com.consultorio.odonto.adaptador.saida.persistencia.PersistenciaDynamoDb;
import br.com.consultorio.odonto.adaptador.saida.persistencia.cripto.CriptografiaService;
import br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb.Atributos.Item;
import br.com.consultorio.odonto.aplicacao.porta.saida.CifradorDocumento;
import br.com.consultorio.odonto.aplicacao.porta.saida.PacienteRepositorio;
import br.com.consultorio.odonto.dominio.modelo.DocumentoRg;
import br.com.consultorio.odonto.dominio.modelo.Paciente;
import br.com.consultorio.odonto.dominio.modelo.WaId;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pacientes no DynamoDB, com os dados pessoais cifrados (AES-GCM) como no PostgreSQL. A unicidade do RG é garantida
 * por um item-trava {@code RG#<hmac>} gravado com condição na mesma transação do paciente.
 */
@Repository
@PersistenciaDynamoDb
public class PacienteDynamoDbAdapter implements PacienteRepositorio {

    private static final String TIPO = "PACIENTE";
    private static final String SEPARADOR_RG = "|";

    private final TabelaDynamoDb tabela;
    private final CifradorDocumento cifrador;
    private final CriptografiaService cripto;

    public PacienteDynamoDbAdapter(TabelaDynamoDb tabela, CifradorDocumento cifrador, CriptografiaService cripto) {
        this.tabela = tabela;
        this.cifrador = cifrador;
        this.cripto = cripto;
    }

    static Chave chave(long id) {
        return new Chave(TIPO + "#" + id, TIPO);
    }

    private static Chave chaveRg(String rgHash) {
        return new Chave("RG#" + rgHash, "RG");
    }

    @Override
    public Paciente salvar(Paciente paciente) {
        long id = paciente.id().orElseGet(() -> tabela.proximoValor("paciente"));
        String rgHash = cifrador.hash(paciente.rg());
        Map<String, AttributeValue> item = new Item()
                .chave(chave(id))
                .indice(TabelaDynamoDb.Indice.GSI1, TIPO, Atributos.ordenavel(id))
                .com("id", id)
                .com("rgHash", rgHash)
                .com("rg", cripto.cifrar(paciente.rg().chaveCanonica()))
                .com("nome", cripto.cifrar(paciente.nomeCompleto()))
                .com("dataNascimento", cripto.cifrar(paciente.dataNascimento().toString()))
                .com("responsavel", cripto.cifrar(paciente.responsavelLegal().orElse(null)))
                .comTextos("telefones", paciente.telefones().stream().map(WaId::valor).toList())
                .com("consentimentoLgpd", paciente.consentimentoLgpd())
                .com("dataConsentimento", paciente.dataConsentimento().orElse(null))
                .com("dataCadastro", paciente.dataCadastro())
                .mapa();
        tabela.gravar(new Item().chave(chaveRg(rgHash)).com("pacienteId", id).mapa(),
                Condicao.de("attribute_not_exists(pk) OR pacienteId = :id", Map.of(":id", AttributeValue.fromN(
                        Long.toString(id))), () -> new IllegalStateException("RG já cadastrado para outro paciente.")));
        tabela.gravar(item);
        return paraDominio(item);
    }

    @Override
    public Optional<Paciente> buscarPorId(long id) {
        return tabela.buscar(chave(id)).map(this::paraDominio);
    }

    @Override
    public Optional<Paciente> buscarPorRgHash(String rgHash) {
        return tabela.buscar(chaveRg(rgHash)).map(trava -> Atributos.numero(trava, "pacienteId"))
                .flatMap(this::buscarPorId);
    }

    @Override
    public List<Paciente> listarTodos() {
        return tabela.consultar(TabelaDynamoDb.Indice.GSI1, TIPO, null, null).stream().map(this::paraDominio).toList();
    }

    private Paciente paraDominio(Map<String, AttributeValue> item) {
        String rgCanonico = cripto.decifrar(Atributos.texto(item, "rg"));
        int separador = rgCanonico.lastIndexOf(SEPARADOR_RG);
        DocumentoRg rg = DocumentoRg.de(rgCanonico.substring(0, separador), rgCanonico.substring(separador + 1));
        return Paciente.restaurar(Atributos.numero(item, "id"), rg, cripto.decifrar(Atributos.texto(item, "nome")),
                LocalDate.parse(cripto.decifrar(Atributos.texto(item, "dataNascimento"))),
                cripto.decifrar(Atributos.texto(item, "responsavel")),
                Atributos.textos(item, "telefones").stream().map(WaId::new).collect(Collectors.toSet()),
                Atributos.logico(item, "consentimentoLgpd"), Atributos.instante(item, "dataConsentimento"),
                Atributos.instante(item, "dataCadastro"));
    }
}
