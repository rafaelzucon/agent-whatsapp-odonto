package br.com.consultorio.odonto.aplicacao.porta.entrada;

/** Correção manual da classificação de uma queixa pelo dentista (origem MANUAL). */
public interface ReclassificarQueixaUseCase {

    QueixaVisao reclassificar(long queixaId, String codigoProblema);
}
