package br.com.consultorio.odonto.adaptador.saida.notificacao;

import java.util.Objects;

/**
 * Como avisar o consultório pelo WhatsApp.
 *
 * @param usarTemplate        {@code true} (recomendado): usa templates aprovados, que funcionam fora da janela de 24h;
 *                            {@code false}: envia texto livre (só entrega se o número do consultório falou com o
 *                            número do agente nas últimas 24h)
 * @param templateAgendamento nome do template com 4 parâmetros (paciente, data e horário, problema, prioridade)
 * @param templateAlerta      nome do template com 3 parâmetros (tipo do alerta, paciente, resumo)
 * @param idioma              código de idioma do template, ex.: {@code pt_BR}
 */
public record ConfiguracaoNotificacaoWhatsApp(boolean usarTemplate, String templateAgendamento, String templateAlerta,
                                              String idioma) {

    public ConfiguracaoNotificacaoWhatsApp {
        Objects.requireNonNull(templateAgendamento, "templateAgendamento");
        Objects.requireNonNull(templateAlerta, "templateAlerta");
        Objects.requireNonNull(idioma, "idioma");
    }
}
