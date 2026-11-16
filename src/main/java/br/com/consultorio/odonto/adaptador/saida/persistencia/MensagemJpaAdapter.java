package br.com.consultorio.odonto.adaptador.saida.persistencia;

import br.com.consultorio.odonto.adaptador.saida.persistencia.entidade.MensagemEntity;
import br.com.consultorio.odonto.adaptador.saida.persistencia.jpa.MensagemJpaRepository;
import br.com.consultorio.odonto.aplicacao.excecao.MensagemDuplicadaException;
import br.com.consultorio.odonto.aplicacao.porta.saida.MensagemRepositorio;
import br.com.consultorio.odonto.dominio.modelo.Mensagem;
import br.com.consultorio.odonto.dominio.modelo.WaId;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/** Histórico de mensagens; o {@code wa_message_id} único garante a idempotência do webhook. */
@Repository
@Transactional(readOnly = true)
public class MensagemJpaAdapter implements MensagemRepositorio {

    private final MensagemJpaRepository repositorio;

    public MensagemJpaAdapter(MensagemJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional
    public Mensagem salvar(Mensagem mensagem) {
        MensagemEntity entidade = new MensagemEntity(mensagem.id().orElse(null), mensagem.waMessageId().orElse(null),
                mensagem.waId().valor(), mensagem.direcao(), mensagem.tipo(), mensagem.texto().orElse(null),
                mensagem.midiaId().orElse(null), mensagem.automatica(), mensagem.criadaEm());
        try {
            return paraDominio(repositorio.saveAndFlush(entidade));
        } catch (DataIntegrityViolationException e) {
            if (!ViolacaoRestricao.envolve(e, "wa_message_id")) {
                throw e;
            }
            throw new MensagemDuplicadaException("Mensagem " + mensagem.waMessageId().orElse("") + " já recebida.");
        }
    }

    @Override
    public boolean existePorWaMessageId(String waMessageId) {
        return repositorio.existsByWaMessageId(waMessageId);
    }

    @Override
    public Optional<Mensagem> buscarPorId(long id) {
        return repositorio.findById(id).map(MensagemJpaAdapter::paraDominio);
    }

    @Override
    @Transactional
    public int removerAnterioresA(Instant limite) {
        return repositorio.removerAnterioresA(limite);
    }

    private static Mensagem paraDominio(MensagemEntity e) {
        return Mensagem.restaurar(e.getId(), e.getWaMessageId(), new WaId(e.getWaId()), e.getDirecao(), e.getTipo(),
                e.getTexto(), e.getMidiaId(), e.isAutomatica(), e.getCriadaEm());
    }
}
