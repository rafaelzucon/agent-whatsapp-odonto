package br.com.consultorio.odonto.aplicacao.porta.entrada;

/** Apaga conversas em andamento paradas há mais que o tempo de expiração (descarta dados parciais). */
public interface ExpirarConversasUseCase {

    /** @return quantas conversas foram apagadas */
    int expirar();
}
