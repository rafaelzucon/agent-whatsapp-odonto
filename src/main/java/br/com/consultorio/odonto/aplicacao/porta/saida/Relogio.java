package br.com.consultorio.odonto.aplicacao.porta.saida;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Fonte de tempo da aplicação (substituível por um relógio fixo nos testes). */
public interface Relogio {

    Instant agora();

    /** Fuso do consultório (ex.: America/Sao_Paulo). */
    ZoneId fuso();

    default LocalDateTime agoraLocal() {
        return LocalDateTime.ofInstant(agora(), fuso());
    }

    default LocalDate hoje() {
        return agoraLocal().toLocalDate();
    }
}
