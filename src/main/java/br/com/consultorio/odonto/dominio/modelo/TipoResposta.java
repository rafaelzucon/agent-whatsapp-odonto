package br.com.consultorio.odonto.dominio.modelo;

/**
 * Mensagens fixas que o agente envia ao paciente. Os textos são configuráveis fora do domínio.
 */
public enum TipoResposta {
    BOAS_VINDAS,
    PACIENTE_ENCONTRADO,
    CONSULTA_EXISTENTE,
    PEDIR_PROBLEMA,
    ENCERRAMENTO,
    PEDIR_NOME,
    PEDIR_DATA_NASCIMENTO,
    PEDIR_RESPONSAVEL,
    PEDIR_CONSENTIMENTO,
    SEM_CONSENTIMENTO,
    CADASTRO_CONCLUIDO,
    EMERGENCIA,
    PEDIR_DATA_HORA,
    SUGERIR_HORARIO,
    OFERECER_HORARIO,
    AGENDADO,
    NAO_ENTENDI,
    ATENDIMENTO_HUMANO,
    SEM_HORARIO_LIVRE,
    PEDIR_TEXTO
}
