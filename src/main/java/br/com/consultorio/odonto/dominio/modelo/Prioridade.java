package br.com.consultorio.odonto.dominio.modelo;

/**
 * Prioridade de atendimento, da mais alta (EMERGENCIA) para a mais baixa (ROTINA).
 */
public enum Prioridade {
    EMERGENCIA("Emergência"),
    URGENCIA("Urgência"),
    ROTINA("Rotina");

    private final String descricao;

    Prioridade(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }

    public boolean maisAltaQue(Prioridade outra) {
        return ordinal() < outra.ordinal();
    }

    /** Devolve a mais alta entre esta e a outra. */
    public Prioridade maisAltaEntre(Prioridade outra) {
        return outra != null && outra.maisAltaQue(this) ? outra : this;
    }
}
