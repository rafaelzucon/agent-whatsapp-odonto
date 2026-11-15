package br.com.consultorio.odonto.aplicacao.porta.entrada;

import br.com.consultorio.odonto.dominio.modelo.DocumentoRg;
import br.com.consultorio.odonto.dominio.modelo.WaId;

import java.time.LocalDateTime;
import java.util.Optional;

/** Busca o paciente pelo RG e vincula o número de WhatsApp a ele, se ainda não estiver vinculado. */
public interface IdentificarPacienteUseCase {

    Optional<PacienteIdentificado> identificar(DocumentoRg rg, WaId waId);

    record PacienteIdentificado(long pacienteId, String primeiroNome, Optional<LocalDateTime> consultaFutura) {
    }
}
