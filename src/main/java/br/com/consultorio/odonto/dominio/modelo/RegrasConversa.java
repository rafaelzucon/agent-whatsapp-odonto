package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;

import java.time.Duration;
import java.util.Objects;

/**
 * Parâmetros da máquina de estados.
 *
 * @param expiracao             inatividade após a qual a conversa recomeça do início
 * @param maxTentativasPorEtapa respostas inválidas toleradas na mesma etapa antes do atendimento humano
 * @param confirmarSugestao     se true, pede confirmação antes de agendar um horário sugerido
 */
public record RegrasConversa(Duration expiracao, int maxTentativasPorEtapa, boolean confirmarSugestao) {

    public RegrasConversa {
        Objects.requireNonNull(expiracao, "expiracao");
        if (maxTentativasPorEtapa < 1) {
            throw new DadoInvalidoException("O máximo de tentativas por etapa deve ser ao menos 1.");
        }
    }
}
