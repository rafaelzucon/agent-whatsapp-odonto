package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Expediente do consultório e regras da grade de horários.
 */
public record HorarioFuncionamento(
        Set<DayOfWeek> diasAtendimento,
        LocalTime abertura,
        LocalTime fechamento,
        LocalTime almocoInicio,
        LocalTime almocoFim,
        Duration duracaoConsulta,
        Duration antecedenciaMinima) {

    public HorarioFuncionamento {
        Objects.requireNonNull(abertura, "abertura");
        Objects.requireNonNull(fechamento, "fechamento");
        Objects.requireNonNull(duracaoConsulta, "duracaoConsulta");
        Objects.requireNonNull(antecedenciaMinima, "antecedenciaMinima");
        if (diasAtendimento == null || diasAtendimento.isEmpty()) {
            throw new DadoInvalidoException("Informe ao menos um dia de atendimento.");
        }
        diasAtendimento = Set.copyOf(EnumSet.copyOf(diasAtendimento));
        if (!fechamento.isAfter(abertura)) {
            throw new DadoInvalidoException("O fechamento deve ser depois da abertura.");
        }
        if ((almocoInicio == null) != (almocoFim == null)) {
            throw new DadoInvalidoException("Informe início e fim do almoço, ou nenhum dos dois.");
        }
        if (almocoInicio != null && (!almocoFim.isAfter(almocoInicio)
                || almocoInicio.isBefore(abertura) || almocoFim.isAfter(fechamento))) {
            throw new DadoInvalidoException("O intervalo de almoço deve estar dentro do expediente.");
        }
        if (duracaoConsulta.isZero() || duracaoConsulta.isNegative()) {
            throw new DadoInvalidoException("A duração da consulta deve ser positiva.");
        }
    }

    public boolean atendeNo(DayOfWeek dia) {
        return diasAtendimento.contains(dia);
    }

    public boolean temAlmoco() {
        return almocoInicio != null;
    }

    /** Inícios de consulta da grade para a data, em ordem, sem considerar feriados ou ocupação. */
    public List<LocalDateTime> grade(LocalDate data) {
        List<LocalDateTime> inicios = new ArrayList<>();
        if (!atendeNo(data.getDayOfWeek())) {
            return inicios;
        }
        if (temAlmoco()) {
            adicionarBloco(data, abertura, almocoInicio, inicios);
            adicionarBloco(data, almocoFim, fechamento, inicios);
        } else {
            adicionarBloco(data, abertura, fechamento, inicios);
        }
        return inicios;
    }

    private void adicionarBloco(LocalDate data, LocalTime de, LocalTime ate, List<LocalDateTime> inicios) {
        LocalDateTime cursor = data.atTime(de);
        LocalDateTime limite = data.atTime(ate);
        while (!cursor.plus(duracaoConsulta).isAfter(limite)) {
            inicios.add(cursor);
            cursor = cursor.plus(duracaoConsulta);
        }
    }
}
