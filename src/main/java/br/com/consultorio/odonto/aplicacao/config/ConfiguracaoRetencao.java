package br.com.consultorio.odonto.aplicacao.config;

/**
 * Prazos de retenção (LGPD) das mensagens de WhatsApp e das conversas encerradas.
 * Pacientes, prontuários, queixas e consultas nunca são apagados automaticamente.
 */
public record ConfiguracaoRetencao(int mensagensDias, int conversasEncerradasDias) {

    public ConfiguracaoRetencao {
        if (mensagensDias < 1 || conversasEncerradasDias < 1) {
            throw new IllegalArgumentException("Os prazos de retenção devem ser positivos.");
        }
    }
}
