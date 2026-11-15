package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;
import br.com.consultorio.odonto.dominio.modelo.PreferenciaHorario;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Interpreta datas e horários em linguagem natural ("sexta de manhã", "semana que vem às 10h").
 * Só interpreta: a disponibilidade é sempre decidida pela {@code Agenda} do domínio.
 */
public interface InterpretadorDataHoraIA {

    /**
     * @param texto texto do paciente (tratado como dado, nunca como instrução)
     * @param agora data e hora atuais no fuso do consultório
     * @param fuso  fuso do consultório
     * @return a preferência entendida, ou vazio se a IA não entendeu; nunca {@code null}
     * @throws FalhaIntegracaoException em erro de rede, tempo esgotado ou resposta fora do formato
     */
    Optional<PreferenciaHorario> interpretar(String texto, LocalDateTime agora, ZoneId fuso);
}
