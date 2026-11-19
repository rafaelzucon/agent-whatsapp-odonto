package br.com.consultorio.odonto.adaptador.saida.persistencia;

import br.com.consultorio.odonto.adaptador.saida.persistencia.entidade.ProntuarioEntity;
import br.com.consultorio.odonto.adaptador.saida.persistencia.entidade.QueixaRelatadaEntity;
import br.com.consultorio.odonto.adaptador.saida.persistencia.jpa.ProntuarioJpaRepository;
import br.com.consultorio.odonto.aplicacao.excecao.RecursoNaoEncontradoException;
import br.com.consultorio.odonto.aplicacao.porta.saida.CatalogoProblemasRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ProntuarioRepositorio;
import br.com.consultorio.odonto.dominio.modelo.ProblemaOdontologico;
import br.com.consultorio.odonto.dominio.modelo.Prontuario;
import br.com.consultorio.odonto.dominio.modelo.QueixaRelatada;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Prontuários e queixas. Queixas novas são inseridas; as existentes só têm classificação e consulta atualizadas. */
@Repository
@PersistenciaJpa
@Transactional(readOnly = true)
public class ProntuarioJpaAdapter implements ProntuarioRepositorio {

    private final ProntuarioJpaRepository repositorio;
    private final CatalogoProblemasRepositorio catalogo;

    public ProntuarioJpaAdapter(ProntuarioJpaRepository repositorio, CatalogoProblemasRepositorio catalogo) {
        this.repositorio = repositorio;
        this.catalogo = catalogo;
    }

    @Override
    @Transactional
    public Prontuario salvar(Prontuario prontuario) {
        ProntuarioEntity entidade = prontuario.id()
                .map(id -> repositorio.findById(id).orElseThrow(
                        () -> new RecursoNaoEncontradoException("Prontuário " + id + " não encontrado.")))
                .orElseGet(() -> new ProntuarioEntity(prontuario.pacienteId(), prontuario.numero(),
                        prontuario.dataAbertura()));
        Map<Long, QueixaRelatadaEntity> existentes = entidade.getQueixas().stream()
                .collect(Collectors.toMap(QueixaRelatadaEntity::getId, Function.identity()));
        for (QueixaRelatada queixa : prontuario.queixas()) {
            QueixaRelatadaEntity alvo = queixa.id().map(existentes::get).orElse(null);
            if (alvo == null) {
                alvo = new QueixaRelatadaEntity(queixa.textoOriginal(), queixa.dataRegistro());
                entidade.adicionar(alvo);
            }
            alvo.atualizar(queixa.problema().codigo(), queixa.prioridade(), queixa.confianca(), queixa.origem(),
                    queixa.consultaId().orElse(null));
        }
        return paraDominio(repositorio.saveAndFlush(entidade));
    }

    @Override
    @Transactional
    public long proximoSequencial() {
        return repositorio.proximoNumero();
    }

    @Override
    public Optional<Prontuario> buscarPorPacienteId(long pacienteId) {
        return repositorio.findByPacienteId(pacienteId).map(this::paraDominio);
    }

    @Override
    public Optional<Prontuario> buscarPorQueixaId(long queixaId) {
        return repositorio.findByQueixaId(queixaId).map(this::paraDominio);
    }

    private Prontuario paraDominio(ProntuarioEntity e) {
        Map<String, ProblemaOdontologico> problemas = new HashMap<>();
        List<QueixaRelatada> queixas = new ArrayList<>();
        for (QueixaRelatadaEntity q : e.getQueixas()) {
            ProblemaOdontologico problema = problemas.computeIfAbsent(q.getProblemaCodigo(),
                    codigo -> catalogo.buscarPorCodigo(codigo).orElseThrow(
                            () -> new IllegalStateException("Problema " + codigo + " ausente do catálogo.")));
            queixas.add(QueixaRelatada.restaurar(q.getId(), q.getTexto(), problema, q.getPrioridade(),
                    q.getConfianca(), q.getOrigem(), q.getDataRegistro(), q.getConsultaId()));
        }
        return Prontuario.restaurar(e.getId(), e.getPacienteId(), e.getNumero(), e.getDataAbertura(), queixas);
    }
}
