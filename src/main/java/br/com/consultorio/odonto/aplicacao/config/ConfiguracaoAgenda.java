package br.com.consultorio.odonto.aplicacao.config;

import br.com.consultorio.odonto.dominio.modelo.HorarioFuncionamento;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

/**
 * Configuração da agenda única do consultório.
 *
 * @param funcionamento   dias, horários, intervalo de almoço, duração da consulta e antecedência mínima
 * @param feriados        datas sem atendimento
 * @param diasBuscaMaxima até quantos dias à frente procurar horário livre
 */
public record ConfiguracaoAgenda(HorarioFuncionamento funcionamento, Set<LocalDate> feriados, int diasBuscaMaxima) {

    public ConfiguracaoAgenda {
        Objects.requireNonNull(funcionamento, "funcionamento");
        feriados = Set.copyOf(feriados);
        if (diasBuscaMaxima < 1) {
            throw new IllegalArgumentException("diasBuscaMaxima deve ser positivo.");
        }
    }
}
