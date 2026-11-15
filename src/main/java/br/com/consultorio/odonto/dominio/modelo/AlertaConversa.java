package br.com.consultorio.odonto.dominio.modelo;

import java.util.Objects;

/**
 * Aviso que a conversa pede para enviar ao consultório (emergência, consentimento recusado, atendimento humano).
 *
 * @param identificacao nome do paciente ou final do número de WhatsApp
 */
public record AlertaConversa(TipoAlerta tipo, String identificacao, String resumo, Prioridade prioridade) {

    public AlertaConversa {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(identificacao, "identificacao");
        Objects.requireNonNull(resumo, "resumo");
        Objects.requireNonNull(prioridade, "prioridade");
    }
}
