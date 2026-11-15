package br.com.consultorio.odonto.dominio.modelo;

import java.time.LocalTime;

/**
 * Período do dia preferido pelo paciente.
 */
public enum Periodo {
    MANHA(LocalTime.MIN, LocalTime.NOON),
    TARDE(LocalTime.NOON, LocalTime.of(18, 0)),
    NOITE(LocalTime.of(18, 0), LocalTime.MAX);

    private final LocalTime inicio;
    private final LocalTime fim;

    Periodo(LocalTime inicio, LocalTime fim) {
        this.inicio = inicio;
        this.fim = fim;
    }

    public LocalTime inicio() {
        return inicio;
    }

    public boolean contem(LocalTime hora) {
        return !hora.isBefore(inicio) && hora.isBefore(fim);
    }
}
