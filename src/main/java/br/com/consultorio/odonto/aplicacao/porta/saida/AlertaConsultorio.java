package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.dominio.modelo.AlertaConversa;
import br.com.consultorio.odonto.dominio.modelo.Prioridade;
import br.com.consultorio.odonto.dominio.modelo.TipoAlerta;

import java.util.List;
import java.util.Objects;

/** Alerta ao consultório: emergência, consentimento recusado ou atendimento humano necessário. */
public record AlertaConsultorio(TipoAlerta tipo, String identificacao, String resumo, Prioridade prioridade) {

    public AlertaConsultorio {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(identificacao, "identificacao");
        Objects.requireNonNull(resumo, "resumo");
        Objects.requireNonNull(prioridade, "prioridade");
    }

    public static AlertaConsultorio de(AlertaConversa alerta) {
        return new AlertaConsultorio(alerta.tipo(), alerta.identificacao(), alerta.resumo(), alerta.prioridade());
    }

    /** Os 3 parâmetros do template alerta_consultorio: tipo do alerta, paciente, resumo. */
    public List<String> parametrosTemplate() {
        return List.of(tipo.descricao(), identificacao, resumo);
    }

    public String texto() {
        return "%s (prioridade %s)%nPaciente: %s%nResumo: %s"
                .formatted(tipo.descricao(), prioridade.descricao(), identificacao, resumo);
    }
}
