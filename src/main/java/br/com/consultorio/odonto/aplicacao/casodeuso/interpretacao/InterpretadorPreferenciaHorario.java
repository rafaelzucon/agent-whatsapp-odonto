package br.com.consultorio.odonto.aplicacao.casodeuso.interpretacao;

import br.com.consultorio.odonto.dominio.modelo.PreferenciaHorario;

import java.util.Optional;

/**
 * Estratégia que transforma o texto livre do paciente ("amanhã às 10h") em uma preferência de horário.
 * Novas formas de interpretar entram como novas implementações, sem alterar quem as usa (OCP).
 */
@FunctionalInterface
public interface InterpretadorPreferenciaHorario {

    /** Devolve a preferência entendida ou vazio se esta estratégia não entendeu o texto. */
    Optional<PreferenciaHorario> interpretar(String texto);
}
