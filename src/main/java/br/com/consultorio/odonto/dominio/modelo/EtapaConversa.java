package br.com.consultorio.odonto.dominio.modelo;

/**
 * Etapas (estados) da máquina de estados da conversa.
 */
public enum EtapaConversa {
    AGUARDANDO_RG,
    AGUARDANDO_NOME,
    AGUARDANDO_DATA_NASCIMENTO,
    AGUARDANDO_RESPONSAVEL,
    AGUARDANDO_CONSENTIMENTO,
    /** Paciente identificado que já tem consulta futura: perguntamos se quer agendar outra. */
    AGUARDANDO_CONFIRMACAO_NOVA_CONSULTA,
    AGUARDANDO_PROBLEMA,
    AGUARDANDO_DATA_HORA,
    AGUARDANDO_CONFIRMACAO_SUGESTAO,
    CONCLUIDA,
    ENCERRADA_SEM_CONSENTIMENTO,
    ENCAMINHADA_ATENDIMENTO_HUMANO;

    public boolean ehFinal() {
        return this == CONCLUIDA || this == ENCERRADA_SEM_CONSENTIMENTO || this == ENCAMINHADA_ATENDIMENTO_HUMANO;
    }

    /** Etapas finais em que o agente fica em silêncio, pois a equipe do consultório assume o contato. */
    public boolean aguardaEquipe() {
        return this == ENCERRADA_SEM_CONSENTIMENTO || this == ENCAMINHADA_ATENDIMENTO_HUMANO;
    }
}
