package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.dominio.modelo.Prioridade;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Conteúdo do aviso de nova consulta enviado ao consultório. */
public record AvisoAgendamento(long consultaId, String nomePaciente, LocalDateTime inicio, String problema,
                               Prioridade prioridade, String numeroProntuario) {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"));

    public AvisoAgendamento {
        Objects.requireNonNull(nomePaciente, "nomePaciente");
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(problema, "problema");
        Objects.requireNonNull(prioridade, "prioridade");
        Objects.requireNonNull(numeroProntuario, "numeroProntuario");
    }

    public String dataHoraFormatada() {
        return FORMATO.format(inicio);
    }

    /** Os 4 parâmetros do template novo_agendamento: nome, data e horário, problema, prioridade. */
    public List<String> parametrosTemplate() {
        return List.of(nomePaciente, dataHoraFormatada(), problema, prioridade.descricao());
    }

    public String texto() {
        return "Nova consulta agendada pelo WhatsApp.%nPaciente: %s%nData e horário: %s%nProblema: %s%nPrioridade: %s%nProntuário: %s"
                .formatted(nomePaciente, dataHoraFormatada(), problema, prioridade.descricao(), numeroProntuario);
    }
}
