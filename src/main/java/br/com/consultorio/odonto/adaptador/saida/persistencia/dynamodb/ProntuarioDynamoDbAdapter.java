package br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb;

import br.com.consultorio.odonto.adaptador.saida.persistencia.PersistenciaDynamoDb;
import br.com.consultorio.odonto.adaptador.saida.persistencia.cripto.CriptografiaService;
import br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb.Atributos.Item;
import br.com.consultorio.odonto.aplicacao.excecao.RecursoNaoEncontradoException;
import br.com.consultorio.odonto.aplicacao.porta.saida.CatalogoProblemasRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ProntuarioRepositorio;
import br.com.consultorio.odonto.dominio.modelo.OrigemClassificacao;
import br.com.consultorio.odonto.dominio.modelo.Prioridade;
import br.com.consultorio.odonto.dominio.modelo.ProblemaOdontologico;
import br.com.consultorio.odonto.dominio.modelo.Prontuario;
import br.com.consultorio.odonto.dominio.modelo.QueixaRelatada;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Prontuário (um por paciente) com as queixas embutidas no mesmo item, o que dispensa junções. Cada queixa tem
 * ainda um item {@code QUEIXA#<id>} que aponta para o paciente, usado na busca por id da queixa.
 */
@Repository
@PersistenciaDynamoDb
public class ProntuarioDynamoDbAdapter implements ProntuarioRepositorio {

    private static final String TIPO = "PRONTUARIO";

    private final TabelaDynamoDb tabela;
    private final CatalogoProblemasRepositorio catalogo;
    private final CriptografiaService cripto;

    public ProntuarioDynamoDbAdapter(TabelaDynamoDb tabela, CatalogoProblemasRepositorio catalogo,
                                     CriptografiaService cripto) {
        this.tabela = tabela;
        this.catalogo = catalogo;
        this.cripto = cripto;
    }

    private static Chave chave(long pacienteId) {
        return new Chave("PACIENTE#" + pacienteId, TIPO);
    }

    private static Chave chaveQueixa(long queixaId) {
        return new Chave("QUEIXA#" + queixaId, "QUEIXA");
    }

    @Override
    public Prontuario salvar(Prontuario prontuario) {
        long id = prontuario.id().orElseGet(() -> tabela.proximoValor("prontuario"));
        List<Map<String, AttributeValue>> queixas = new ArrayList<>();
        for (QueixaRelatada queixa : prontuario.queixas()) {
            long queixaId = queixa.id().orElseGet(() -> {
                long nova = tabela.proximoValor("queixa");
                tabela.gravar(new Item().chave(chaveQueixa(nova)).com("pacienteId", prontuario.pacienteId()).mapa());
                return nova;
            });
            queixas.add(new Item()
                    .com("id", queixaId)
                    .com("texto", cripto.cifrar(queixa.textoOriginal()))
                    .com("problema", queixa.problema().codigo())
                    .com("prioridade", queixa.prioridade())
                    .com("confianca", queixa.confianca())
                    .com("origem", queixa.origem())
                    .com("dataRegistro", queixa.dataRegistro())
                    .com("consultaId", queixa.consultaId().orElse(null))
                    .mapa());
        }
        Map<String, AttributeValue> item = new Item()
                .chave(chave(prontuario.pacienteId()))
                .com("id", id)
                .com("pacienteId", prontuario.pacienteId())
                .com("numero", prontuario.numero())
                .com("dataAbertura", prontuario.dataAbertura())
                .comMapas("queixas", queixas)
                .mapa();
        Condicao condicao = prontuario.id().isPresent()
                ? Condicao.de("id = :id", Map.of(":id", AttributeValue.fromN(Long.toString(id))),
                () -> new RecursoNaoEncontradoException("Prontuário " + id + " não encontrado."))
                : Condicao.de("attribute_not_exists(pk)",
                () -> new IllegalStateException("O paciente " + prontuario.pacienteId() + " já tem prontuário."));
        tabela.gravar(item, condicao);
        return paraDominio(item);
    }

    @Override
    public long proximoSequencial() {
        return tabela.proximoValor("prontuario-numero");
    }

    @Override
    public Optional<Prontuario> buscarPorPacienteId(long pacienteId) {
        return tabela.buscar(chave(pacienteId)).map(this::paraDominio);
    }

    @Override
    public Optional<Prontuario> buscarPorQueixaId(long queixaId) {
        return tabela.buscar(chaveQueixa(queixaId)).map(indice -> Atributos.numero(indice, "pacienteId"))
                .flatMap(this::buscarPorPacienteId);
    }

    private Prontuario paraDominio(Map<String, AttributeValue> item) {
        Map<String, ProblemaOdontologico> problemas = new HashMap<>();
        List<QueixaRelatada> queixas = new ArrayList<>();
        for (Map<String, AttributeValue> q : Atributos.mapas(item, "queixas")) {
            ProblemaOdontologico problema = problemas.computeIfAbsent(Atributos.texto(q, "problema"),
                    codigo -> catalogo.buscarPorCodigo(codigo).orElseThrow(
                            () -> new IllegalStateException("Problema " + codigo + " ausente do catálogo.")));
            queixas.add(QueixaRelatada.restaurar(Atributos.numero(q, "id"), cripto.decifrar(Atributos.texto(q, "texto")),
                    problema, Prioridade.valueOf(Atributos.texto(q, "prioridade")), Atributos.decimal(q, "confianca"),
                    OrigemClassificacao.valueOf(Atributos.texto(q, "origem")), Atributos.instante(q, "dataRegistro"),
                    Atributos.numero(q, "consultaId")));
        }
        return Prontuario.restaurar(Atributos.numero(item, "id"), Atributos.numero(item, "pacienteId"),
                Atributos.texto(item, "numero"), Atributos.instante(item, "dataAbertura"), queixas);
    }
}
