package br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb;

import br.com.consultorio.odonto.adaptador.saida.persistencia.FusoPersistencia;
import br.com.consultorio.odonto.adaptador.saida.persistencia.PersistenciaDynamoDb;
import br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb.Atributos.Item;
import br.com.consultorio.odonto.aplicacao.porta.saida.BloqueioAgendaRepositorio;
import br.com.consultorio.odonto.dominio.modelo.BloqueioAgenda;
import br.com.consultorio.odonto.dominio.modelo.Horario;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Bloqueios da agenda (férias, cursos...) no DynamoDB, ordenados pelo início no índice GSI1. */
@Repository
@PersistenciaDynamoDb
public class BloqueioAgendaDynamoDbAdapter implements BloqueioAgendaRepositorio {

    private static final String TIPO = "BLOQUEIO";

    private final TabelaDynamoDb tabela;
    private final FusoPersistencia fuso;

    public BloqueioAgendaDynamoDbAdapter(TabelaDynamoDb tabela, FusoPersistencia fuso) {
        this.tabela = tabela;
        this.fuso = fuso;
    }

    private static Chave chave(long id) {
        return new Chave(TIPO + "#" + id, TIPO);
    }

    @Override
    public BloqueioAgenda salvar(BloqueioAgenda bloqueio) {
        long id = bloqueio.id().orElseGet(() -> tabela.proximoValor("bloqueio"));
        Instant inicio = fuso.paraInstante(bloqueio.periodo().inicio());
        Map<String, AttributeValue> item = new Item()
                .chave(chave(id))
                .indice(TabelaDynamoDb.Indice.GSI1, TIPO, Atributos.ordenavel(inicio) + "#" + Atributos.ordenavel(id))
                .com("id", id)
                .com("inicio", inicio)
                .com("fim", fuso.paraInstante(bloqueio.periodo().fim()))
                .com("motivo", bloqueio.motivo())
                .mapa();
        tabela.gravar(item);
        return paraDominio(item);
    }

    @Override
    public boolean remover(long id) {
        if (tabela.buscar(chave(id)).isEmpty()) {
            return false;
        }
        tabela.remover(chave(id));
        return true;
    }

    /** Bloqueios que se sobrepõem ao período: começam antes do fim e terminam depois do início. */
    @Override
    public List<BloqueioAgenda> listarEntre(LocalDateTime inicio, LocalDateTime fim) {
        Instant inicioPeriodo = fuso.paraInstante(inicio);
        return tabela.consultar(TabelaDynamoDb.Indice.GSI1, TIPO, null, Atributos.ordenavel(fuso.paraInstante(fim)))
                .stream()
                .filter(item -> Atributos.instante(item, "fim").isAfter(inicioPeriodo))
                .map(this::paraDominio)
                .toList();
    }

    private BloqueioAgenda paraDominio(Map<String, AttributeValue> item) {
        return new BloqueioAgenda(Optional.of(Atributos.numero(item, "id")),
                new Horario(fuso.paraLocal(Atributos.instante(item, "inicio")),
                        fuso.paraLocal(Atributos.instante(item, "fim"))), Atributos.texto(item, "motivo"));
    }
}
