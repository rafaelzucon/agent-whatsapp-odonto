package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.casodeuso.classificacao.ClassificadorQueixa;
import br.com.consultorio.odonto.aplicacao.excecao.RecursoNaoEncontradoException;
import br.com.consultorio.odonto.aplicacao.porta.entrada.RegistrarQueixaUseCase;
import br.com.consultorio.odonto.aplicacao.porta.saida.ExecutorTransacional;
import br.com.consultorio.odonto.aplicacao.porta.saida.ProntuarioRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import br.com.consultorio.odonto.dominio.modelo.ClassificacaoQueixa;
import br.com.consultorio.odonto.dominio.modelo.Prontuario;
import br.com.consultorio.odonto.dominio.modelo.QueixaRelatada;

import java.util.Objects;

/**
 * Registra a queixa no prontuário. A classificação é delegada ao {@link ClassificadorQueixa} (por padrão,
 * IA → palavras-chave → avaliação geral, com o detector de sinais de alarme elevando a prioridade): este caso de
 * uso só orquestra classificar e persistir (SRP).
 */
public final class RegistrarQueixaService implements RegistrarQueixaUseCase {

    private final ProntuarioRepositorio prontuarios;
    private final ClassificadorQueixa classificador;
    private final ExecutorTransacional transacao;
    private final Relogio relogio;

    public RegistrarQueixaService(ProntuarioRepositorio prontuarios, ClassificadorQueixa classificador,
                                  ExecutorTransacional transacao, Relogio relogio) {
        this.prontuarios = Objects.requireNonNull(prontuarios);
        this.classificador = Objects.requireNonNull(classificador);
        this.transacao = Objects.requireNonNull(transacao);
        this.relogio = Objects.requireNonNull(relogio);
    }

    @Override
    public QueixaRegistrada registrar(long pacienteId, String texto) {
        // A classificação (chamada à IA) fica fora da transação para não segurar conexão com o banco.
        ClassificacaoQueixa classificacao = classificador.classificar(texto);
        QueixaRelatada queixa = transacao.executar(() -> {
            Prontuario prontuario = prontuarios.buscarPorPacienteId(pacienteId).orElseThrow(
                    () -> new RecursoNaoEncontradoException("Prontuário do paciente " + pacienteId + " não encontrado."));
            prontuario.registrarQueixa(texto, classificacao, relogio.agora());
            return prontuarios.salvar(prontuario).ultimaQueixa().orElseThrow();
        });
        return new QueixaRegistrada(queixa.id().orElseThrow(), queixa.problema().codigo(), queixa.problema().nome(),
                queixa.prioridade(), queixa.origem(), queixa.confianca());
    }
}
