package br.com.consultorio.odonto.aplicacao.config;

import br.com.consultorio.odonto.dominio.modelo.WaId;

import java.util.Optional;

/**
 * Configuração dos avisos ao consultório.
 *
 * @param numeroNotificacao celular da recepção que recebe os avisos; mensagens vindas dele são ignoradas
 *                          pelo agente, para não abrir uma conversa de paciente com a própria recepção
 */
public record ConfiguracaoNotificacao(Optional<WaId> numeroNotificacao) {

    public boolean ehNumeroDoConsultorio(WaId waId) {
        return numeroNotificacao.filter(waId::equals).isPresent();
    }
}
