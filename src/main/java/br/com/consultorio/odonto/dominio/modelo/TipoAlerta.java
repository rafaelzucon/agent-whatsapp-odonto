package br.com.consultorio.odonto.dominio.modelo;

/**
 * Tipos de alerta enviados ao consultório fora do fluxo normal de agendamento.
 */
public enum TipoAlerta {
    EMERGENCIA("Emergência odontológica", Prioridade.EMERGENCIA),
    CONSENTIMENTO_RECUSADO("Consentimento recusado", Prioridade.ROTINA),
    ATENDIMENTO_HUMANO("Atendimento humano necessário", Prioridade.URGENCIA);

    private final String descricao;
    private final Prioridade prioridade;

    TipoAlerta(String descricao, Prioridade prioridade) {
        this.descricao = descricao;
        this.prioridade = prioridade;
    }

    public String descricao() {
        return descricao;
    }

    public Prioridade prioridade() {
        return prioridade;
    }
}
