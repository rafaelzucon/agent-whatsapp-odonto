package br.com.consultorio.odonto.aplicacao.porta.entrada;

import br.com.consultorio.odonto.dominio.modelo.OrigemClassificacao;
import br.com.consultorio.odonto.dominio.modelo.Prioridade;

/** Classifica a queixa no catálogo (IA com fallback por palavra-chave) e registra no prontuário. */
public interface RegistrarQueixaUseCase {

    QueixaRegistrada registrar(long pacienteId, String texto);

    record QueixaRegistrada(long queixaId, String codigoProblema, String nomeProblema, Prioridade prioridade,
                            OrigemClassificacao origem, double confianca) {
    }
}
