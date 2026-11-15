package br.com.consultorio.odonto.dominio.excecao;

/**
 * Motivo pelo qual um horário pedido não pode ser agendado.
 */
public enum MotivoIndisponibilidade {
    NO_PASSADO("o horário já passou"),
    ANTECEDENCIA_MINIMA("o horário não respeita a antecedência mínima"),
    DIA_SEM_ATENDIMENTO("não há atendimento nesse dia da semana"),
    FERIADO("o dia é feriado"),
    FORA_DO_EXPEDIENTE("o horário está fora do expediente"),
    INTERVALO_ALMOCO("o horário cai no intervalo de almoço"),
    FORA_DA_GRADE("o horário não coincide com o início de uma consulta da grade"),
    OCUPADO("o horário já está ocupado"),
    BLOQUEADO("a agenda está bloqueada nesse horário"),
    ALEM_DO_LIMITE_DE_BUSCA("o horário está além do limite de dias para agendamento");

    private final String descricao;

    MotivoIndisponibilidade(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}
