package br.com.consultorio.odonto.adaptador.entrada.webhook;

/**
 * Segredos do webhook da Meta.
 *
 * @param tokenVerificacao valor combinado no cadastro do webhook (hub.verify_token)
 * @param segredoApp       App Secret do aplicativo da Meta, usado para validar o cabeçalho X-Hub-Signature-256
 * @param phoneNumberId    id do número do consultório; mensagens de outros números da mesma conta são ignoradas
 */
public record ConfiguracaoWebhook(String tokenVerificacao, String segredoApp, String phoneNumberId) {

    public ConfiguracaoWebhook {
        tokenVerificacao = tokenVerificacao == null ? "" : tokenVerificacao.strip();
        segredoApp = segredoApp == null ? "" : segredoApp.strip();
        phoneNumberId = phoneNumberId == null ? "" : phoneNumberId.strip();
    }

    @Override
    public String toString() {
        return "ConfiguracaoWebhook[tokenVerificacao=***, segredoApp=***, phoneNumberId=" + phoneNumberId + "]";
    }
}
