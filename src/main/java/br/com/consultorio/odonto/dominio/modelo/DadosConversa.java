package br.com.consultorio.odonto.dominio.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Dados coletados até o momento na conversa. Imutável: cada etapa devolve uma nova cópia.
 */
public record DadosConversa(
        Optional<DocumentoRg> rg,
        Optional<String> nome,
        Optional<LocalDate> dataNascimento,
        Optional<String> responsavel,
        Optional<Long> pacienteId,
        Optional<String> nomePaciente,
        Optional<String> numeroProntuario,
        Optional<LocalDateTime> consultaExistente,
        Optional<Long> queixaId,
        Optional<String> problema,
        Optional<Prioridade> prioridade,
        Optional<Horario> horarioSugerido) {

    public DadosConversa {
        Objects.requireNonNull(rg);
        Objects.requireNonNull(nome);
        Objects.requireNonNull(dataNascimento);
        Objects.requireNonNull(responsavel);
        Objects.requireNonNull(pacienteId);
        Objects.requireNonNull(nomePaciente);
        Objects.requireNonNull(numeroProntuario);
        Objects.requireNonNull(consultaExistente);
        Objects.requireNonNull(queixaId);
        Objects.requireNonNull(problema);
        Objects.requireNonNull(prioridade);
        Objects.requireNonNull(horarioSugerido);
    }

    public static DadosConversa vazio() {
        return new DadosConversa(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());
    }

    public DadosConversa comRg(DocumentoRg valor) {
        return new DadosConversa(Optional.of(valor), nome, dataNascimento, responsavel, pacienteId, nomePaciente,
                numeroProntuario, consultaExistente, queixaId, problema, prioridade, horarioSugerido);
    }

    public DadosConversa comNome(String valor) {
        return new DadosConversa(rg, Optional.of(valor), dataNascimento, responsavel, pacienteId, nomePaciente,
                numeroProntuario, consultaExistente, queixaId, problema, prioridade, horarioSugerido);
    }

    public DadosConversa comDataNascimento(LocalDate valor) {
        return new DadosConversa(rg, nome, Optional.of(valor), responsavel, pacienteId, nomePaciente,
                numeroProntuario, consultaExistente, queixaId, problema, prioridade, horarioSugerido);
    }

    public DadosConversa comResponsavel(String valor) {
        return new DadosConversa(rg, nome, dataNascimento, Optional.of(valor), pacienteId, nomePaciente,
                numeroProntuario, consultaExistente, queixaId, problema, prioridade, horarioSugerido);
    }

    /** Guarda o paciente identificado e descarta os dados de cadastro temporários. */
    public DadosConversa comPaciente(long id, String nomeDoPaciente, Optional<String> prontuario) {
        return new DadosConversa(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(id), Optional.of(nomeDoPaciente), prontuario, consultaExistente, queixaId, problema,
                prioridade, horarioSugerido);
    }

    public DadosConversa comConsultaExistente(LocalDateTime valor) {
        return new DadosConversa(rg, nome, dataNascimento, responsavel, pacienteId, nomePaciente,
                numeroProntuario, Optional.of(valor), queixaId, problema, prioridade, horarioSugerido);
    }

    public DadosConversa comQueixa(long id, String nomeProblema, Prioridade valor) {
        return new DadosConversa(rg, nome, dataNascimento, responsavel, pacienteId, nomePaciente,
                numeroProntuario, consultaExistente, Optional.of(id), Optional.of(nomeProblema), Optional.of(valor),
                horarioSugerido);
    }

    public DadosConversa comHorarioSugerido(Optional<Horario> valor) {
        return new DadosConversa(rg, nome, dataNascimento, responsavel, pacienteId, nomePaciente,
                numeroProntuario, consultaExistente, queixaId, problema, prioridade, valor);
    }

    public boolean emergencia() {
        return prioridade.filter(p -> p == Prioridade.EMERGENCIA).isPresent();
    }
}
