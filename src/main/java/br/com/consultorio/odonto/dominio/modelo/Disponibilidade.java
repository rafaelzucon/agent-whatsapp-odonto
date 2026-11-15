package br.com.consultorio.odonto.dominio.modelo;

/**
 * Resultado da consulta à agenda para uma preferência do paciente.
 */
public sealed interface Disponibilidade {

    /** O horário exato pedido está livre. */
    record HorarioDisponivel(Horario horario) implements Disponibilidade {
    }

    /**
     * Um horário livre encontrado pela agenda.
     *
     * @param pedidoIndisponivel true se o paciente pediu um horário exato que não estava livre;
     *                           false se o paciente deu só a data ou o período (ou foi uma oferta da agenda)
     */
    record HorarioSugerido(Horario horario, boolean pedidoIndisponivel) implements Disponibilidade {
    }

    /** Não há horário livre dentro do limite de busca. */
    record SemHorarioLivre() implements Disponibilidade {
    }
}
