package br.com.consultorio.odonto.adaptador.saida.whatsapp;

import java.util.Objects;

/**
 * Acesso à WhatsApp Cloud API (Graph API da Meta).
 *
 * @param urlBase              normalmente {@code https://graph.facebook.com}
 * @param versaoApi            versão da Graph API, ex.: {@code v24.0}
 * @param phoneNumberId        id do número do consultório no WhatsApp Manager
 * @param tokenAcesso          token permanente de usuário do sistema (System User)
 * @param tamanhoMaximoMidia   limite, em bytes, para baixar mídias recebidas
 */
public record ConfiguracaoWhatsApp(String urlBase, String versaoApi, String phoneNumberId, String tokenAcesso,
                                   long tamanhoMaximoMidia) {

    public ConfiguracaoWhatsApp {
        Objects.requireNonNull(urlBase, "urlBase");
        Objects.requireNonNull(versaoApi, "versaoApi");
        phoneNumberId = phoneNumberId == null ? "" : phoneNumberId.strip();
        tokenAcesso = tokenAcesso == null ? "" : tokenAcesso.strip();
        if (tamanhoMaximoMidia <= 0) {
            throw new IllegalArgumentException("O tamanho máximo de mídia deve ser positivo.");
        }
    }

    public boolean configurado() {
        return !phoneNumberId.isEmpty() && !tokenAcesso.isEmpty();
    }

    @Override
    public String toString() {
        return "ConfiguracaoWhatsApp[urlBase=" + urlBase + ", versaoApi=" + versaoApi + ", phoneNumberId="
                + phoneNumberId + ", tokenAcesso=***]";
    }
}
