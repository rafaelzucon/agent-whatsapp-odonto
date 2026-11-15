package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.dominio.modelo.DocumentoRg;

/** Calcula o hash determinístico do RG, usado para buscar o paciente sem guardar o RG em texto puro. */
public interface CifradorDocumento {

    /** @return hash em hexadecimal (64 caracteres), sempre igual para o mesmo RG + UF; nunca {@code null} */
    String hash(DocumentoRg rg);
}
