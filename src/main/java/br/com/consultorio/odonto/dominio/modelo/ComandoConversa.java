package br.com.consultorio.odonto.dominio.modelo;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Operações com efeito colateral que a máquina de estados pede para a camada de aplicação executar.
 * O resultado de cada comando volta para a {@link Conversa} por um método específico.
 */
public sealed interface ComandoConversa {

    /** Resultado esperado: {@link Conversa#pacienteEncontrado} ou {@link Conversa#pacienteNaoEncontrado}. */
    record IdentificarPaciente(DocumentoRg rg) implements ComandoConversa {
    }

    /** Resultado esperado: {@link Conversa#pacienteCadastrado}. */
    record CadastrarPaciente(DocumentoRg rg, String nomeCompleto, LocalDate dataNascimento,
                             Optional<String> responsavelLegal) implements ComandoConversa {
    }

    /** Resultado esperado: {@link Conversa#queixaRegistrada}. */
    record RegistrarQueixa(long pacienteId, String texto) implements ComandoConversa {
    }

    /** Resultado esperado: {@link Conversa#disponibilidadeVerificada} ou {@link Conversa#dataHoraNaoEntendida}. */
    record InterpretarDataHora(String texto) implements ComandoConversa {
    }

    /** Resultado esperado: {@link Conversa#disponibilidadeVerificada}. */
    record BuscarPrimeiroHorarioLivre() implements ComandoConversa {
    }

    /** Resultado esperado: {@link Conversa#consultaAgendada} ou {@link Conversa#disponibilidadeVerificada}. */
    record ReservarHorario(long pacienteId, Optional<Long> queixaId, Horario horario) implements ComandoConversa {
    }
}
