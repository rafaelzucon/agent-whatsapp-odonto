package br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb;

import br.com.consultorio.odonto.adaptador.saida.persistencia.FusoPersistencia;
import br.com.consultorio.odonto.adaptador.saida.persistencia.PersistenciaDynamoDb;
import br.com.consultorio.odonto.adaptador.saida.persistencia.dynamodb.Atributos.Item;
import br.com.consultorio.odonto.aplicacao.porta.saida.ConsultaRepositorio;
import br.com.consultorio.odonto.dominio.excecao.HorarioIndisponivelException;
import br.com.consultorio.odonto.dominio.excecao.MotivoIndisponibilidade;
import br.com.consultorio.odonto.dominio.modelo.Consulta;
import br.com.consultorio.odonto.dominio.modelo.Horario;
import br.com.consultorio.odonto.dominio.modelo.StatusConsulta;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Consultas da agenda no DynamoDB. No lugar do índice único parcial do PostgreSQL, cada consulta AGENDADA tem um
 * item-trava {@code HORARIO#<início>} gravado com condição na mesma transação: duas consultas no mesmo horário,
 * mesmo com pedidos simultâneos, resultam em {@link HorarioIndisponivelException}. Cancelar libera a trava.
 */
@Repository
@PersistenciaDynamoDb
public class ConsultaDynamoDbAdapter implements ConsultaRepositorio {

    private static final String TIPO = "CONSULTA";

    private final TabelaDynamoDb tabela;
    private final FusoPersistencia fuso;

    public ConsultaDynamoDbAdapter(TabelaDynamoDb tabela, FusoPersistencia fuso) {
        this.tabela = tabela;
        this.fuso = fuso;
    }

    private static Chave chave(long id) {
        return new Chave(TIPO + "#" + id, TIPO);
    }

    private static Chave chaveHorario(Instant inicio) {
        return new Chave("HORARIO#" + Atributos.ordenavel(inicio), "HORARIO");
    }

    private static String particaoPaciente(long pacienteId) {
        return TIPO + "#PACIENTE#" + pacienteId;
    }

    @Override
    public Consulta salvar(Consulta consulta) {
        long id = consulta.id().orElseGet(() -> tabela.proximoValor("consulta"));
        Instant inicio = fuso.paraInstante(consulta.horario().inicio());
        Map<String, AttributeValue> idValor = Map.of(":id", AttributeValue.fromN(Long.toString(id)));
        consulta.id().map(ConsultaDynamoDbAdapter::chave).flatMap(tabela::buscar)
                .filter(anterior -> StatusConsulta.AGENDADA.name().equals(Atributos.texto(anterior, "status")))
                .map(anterior -> Atributos.instante(anterior, "inicio"))
                .filter(inicioAnterior -> consulta.status() != StatusConsulta.AGENDADA
                        || !inicioAnterior.equals(inicio))
                .ifPresent(inicioAnterior -> tabela.remover(chaveHorario(inicioAnterior),
                        Condicao.de("attribute_not_exists(pk) OR consultaId = :id", idValor,
                                () -> new IllegalStateException("Trava de horário de outra consulta."))));
        if (consulta.status() == StatusConsulta.AGENDADA) {
            tabela.gravar(new Item().chave(chaveHorario(inicio)).com("consultaId", id).mapa(),
                    Condicao.de("attribute_not_exists(pk) OR consultaId = :id", idValor,
                            () -> new HorarioIndisponivelException(MotivoIndisponibilidade.OCUPADO)));
        }
        String ordenacao = Atributos.ordenavel(inicio) + "#" + Atributos.ordenavel(id);
        Map<String, AttributeValue> item = new Item()
                .chave(chave(id))
                .indice(TabelaDynamoDb.Indice.GSI1, TIPO, ordenacao)
                .indice(TabelaDynamoDb.Indice.GSI2, particaoPaciente(consulta.pacienteId()), ordenacao)
                .com("id", id)
                .com("pacienteId", consulta.pacienteId())
                .com("queixaId", consulta.queixaId().orElse(null))
                .com("inicio", inicio)
                .com("fim", fuso.paraInstante(consulta.horario().fim()))
                .com("status", consulta.status())
                .com("criadaEm", consulta.criadaEm())
                .mapa();
        tabela.gravar(item);
        return paraDominio(item);
    }

    @Override
    public Optional<Consulta> buscarPorId(long id) {
        return tabela.buscar(chave(id)).map(this::paraDominio);
    }

    @Override
    public List<Consulta> listarAgendadasEntre(LocalDateTime inicio, LocalDateTime fim) {
        return listarEntre(inicio, fim).stream().filter(c -> c.status() == StatusConsulta.AGENDADA).toList();
    }

    @Override
    public List<Consulta> listarEntre(LocalDateTime inicio, LocalDateTime fim) {
        return tabela.consultar(TabelaDynamoDb.Indice.GSI1, TIPO, Atributos.ordenavel(fuso.paraInstante(inicio)),
                Atributos.ordenavel(fuso.paraInstante(fim))).stream().map(this::paraDominio).toList();
    }

    @Override
    public List<Consulta> listarPorPaciente(long pacienteId) {
        return tabela.consultar(TabelaDynamoDb.Indice.GSI2, particaoPaciente(pacienteId), null, null).stream()
                .map(this::paraDominio).toList();
    }

    @Override
    public Optional<Consulta> proximaAgendadaDoPaciente(long pacienteId, LocalDateTime agora) {
        String depoisDeAgora = Atributos.ordenavel(fuso.paraInstante(agora).plusMillis(1));
        return tabela.consultar(TabelaDynamoDb.Indice.GSI2, particaoPaciente(pacienteId), depoisDeAgora, null).stream()
                .map(this::paraDominio).filter(c -> c.status() == StatusConsulta.AGENDADA).findFirst();
    }

    private Consulta paraDominio(Map<String, AttributeValue> item) {
        return Consulta.restaurar(Atributos.numero(item, "id"), Atributos.numero(item, "pacienteId"),
                Atributos.numero(item, "queixaId"),
                new Horario(fuso.paraLocal(Atributos.instante(item, "inicio")),
                        fuso.paraLocal(Atributos.instante(item, "fim"))),
                StatusConsulta.valueOf(Atributos.texto(item, "status")), Atributos.instante(item, "criadaEm"));
    }
}
