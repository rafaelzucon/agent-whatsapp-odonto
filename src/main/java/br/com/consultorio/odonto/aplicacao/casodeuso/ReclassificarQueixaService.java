package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.excecao.RecursoNaoEncontradoException;
import br.com.consultorio.odonto.aplicacao.porta.entrada.QueixaVisao;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ReclassificarQueixaUseCase;
import br.com.consultorio.odonto.aplicacao.porta.saida.CatalogoProblemasRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ExecutorTransacional;
import br.com.consultorio.odonto.aplicacao.porta.saida.ProntuarioRepositorio;
import br.com.consultorio.odonto.dominio.modelo.ProblemaOdontologico;
import br.com.consultorio.odonto.dominio.modelo.Prontuario;

import java.util.Objects;

/** Correção manual, pela equipe, da classificação feita pela IA ou por palavras-chave. */
public final class ReclassificarQueixaService implements ReclassificarQueixaUseCase {

    private final ProntuarioRepositorio prontuarios;
    private final CatalogoProblemasRepositorio catalogo;
    private final ExecutorTransacional transacao;

    public ReclassificarQueixaService(ProntuarioRepositorio prontuarios, CatalogoProblemasRepositorio catalogo,
                                      ExecutorTransacional transacao) {
        this.prontuarios = Objects.requireNonNull(prontuarios);
        this.catalogo = Objects.requireNonNull(catalogo);
        this.transacao = Objects.requireNonNull(transacao);
    }

    @Override
    public QueixaVisao reclassificar(long queixaId, String codigoProblema) {
        ProblemaOdontologico problema = catalogo.buscarPorCodigo(codigoProblema).orElseThrow(
                () -> new RecursoNaoEncontradoException("Problema " + codigoProblema + " não existe no catálogo."));
        return transacao.executar(() -> {
            Prontuario prontuario = prontuarios.buscarPorQueixaId(queixaId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Queixa " + queixaId + " não encontrada."));
            prontuario.reclassificarQueixa(queixaId, problema);
            return MontadorVisoes.queixa(prontuarios.salvar(prontuario).queixa(queixaId).orElseThrow());
        });
    }
}
