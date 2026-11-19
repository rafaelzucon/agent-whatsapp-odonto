package br.com.consultorio.odonto.adaptador.saida.persistencia;

import br.com.consultorio.odonto.adaptador.saida.persistencia.entidade.ConversaEntity;
import br.com.consultorio.odonto.adaptador.saida.persistencia.jpa.ConversaJpaRepository;
import br.com.consultorio.odonto.aplicacao.porta.saida.ConversaRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.RetencaoConversas;
import br.com.consultorio.odonto.dominio.modelo.Conversa;
import br.com.consultorio.odonto.dominio.modelo.EtapaConversa;
import br.com.consultorio.odonto.dominio.modelo.WaId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/** Estado das conversas. Os dados coletados (RG, nome, nascimento...) são serializados em JSON e cifrados. */
@Repository
@PersistenciaJpa
@Transactional(readOnly = true)
public class ConversaJpaAdapter implements ConversaRepositorio, RetencaoConversas {

    private final ConversaJpaRepository repositorio;
    private final JsonMapper json;

    public ConversaJpaAdapter(ConversaJpaRepository repositorio, JsonMapper json) {
        this.repositorio = repositorio;
        this.json = json;
    }

    @Override
    public Optional<Conversa> buscarPorWaId(WaId waId) {
        return repositorio.findByWaId(waId.valor()).map(this::paraDominio);
    }

    @Override
    @Transactional
    public Conversa salvar(Conversa conversa) {
        // Uma conversa "nova" pode substituir outra já removida/recriada para o mesmo número: reaproveita o id.
        Long id = conversa.id().filter(repositorio::existsById)
                .or(() -> repositorio.findByWaId(conversa.waId().valor()).map(ConversaEntity::getId))
                .orElse(null);
        ConversaEntity entidade = new ConversaEntity(id, conversa.waId().valor(), conversa.etapa(),
                json.writeValueAsString(DadosConversaJson.de(conversa.dados())), conversa.tentativasInvalidas(),
                conversa.ultimaInteracao(), conversa.iniciadaEm());
        return paraDominio(repositorio.saveAndFlush(entidade));
    }

    @Override
    @Transactional
    public int removerInativasDesde(Instant limite, Set<EtapaConversa> etapas) {
        return etapas.isEmpty() ? 0 : repositorio.removerInativas(limite, etapas);
    }

    private Conversa paraDominio(ConversaEntity e) {
        DadosConversaJson dados = json.readValue(e.getDados(), DadosConversaJson.class);
        return Conversa.restaurar(e.getId(), new WaId(e.getWaId()), e.getEtapa(), dados.paraDominio(),
                e.getTentativasInvalidas(), e.getUltimaInteracao(), e.getIniciadaEm());
    }
}
