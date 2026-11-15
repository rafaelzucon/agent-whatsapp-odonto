package br.com.consultorio.odonto.aplicacao.porta.saida;

/** Publica eventos da aplicação. Dentro de uma transação, os ouvintes rodam só depois do commit. */
public interface PublicadorEventos {

    void publicar(Object evento);
}
