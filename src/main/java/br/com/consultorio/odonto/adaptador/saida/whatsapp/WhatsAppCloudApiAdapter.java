package br.com.consultorio.odonto.adaptador.saida.whatsapp;

import br.com.consultorio.odonto.adaptador.saida.whatsapp.CloudApiDtos.Componente;
import br.com.consultorio.odonto.adaptador.saida.whatsapp.CloudApiDtos.EnvioMensagem;
import br.com.consultorio.odonto.adaptador.saida.whatsapp.CloudApiDtos.Idioma;
import br.com.consultorio.odonto.adaptador.saida.whatsapp.CloudApiDtos.InfoMidia;
import br.com.consultorio.odonto.adaptador.saida.whatsapp.CloudApiDtos.MensagemEnviada;
import br.com.consultorio.odonto.adaptador.saida.whatsapp.CloudApiDtos.Parametro;
import br.com.consultorio.odonto.adaptador.saida.whatsapp.CloudApiDtos.RespostaEnvio;
import br.com.consultorio.odonto.adaptador.saida.whatsapp.CloudApiDtos.Template;
import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;
import br.com.consultorio.odonto.aplicacao.porta.saida.EnviadorWhatsApp;
import br.com.consultorio.odonto.aplicacao.porta.saida.MidiaBaixada;
import br.com.consultorio.odonto.aplicacao.porta.saida.ProvedorMidia;
import br.com.consultorio.odonto.dominio.modelo.WaId;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Integração oficial com a WhatsApp Business Platform – Cloud API (Graph API da Meta).
 * Envia textos e templates pelo número do consultório e baixa as mídias recebidas (áudios).
 */
@Component
public class WhatsAppCloudApiAdapter implements EnviadorWhatsApp, ProvedorMidia {

    /** Limite de caracteres do corpo de uma mensagem de texto na Cloud API. */
    static final int LIMITE_TEXTO = 4096;
    private static final int LIMITE_PARAMETRO_TEMPLATE = 1024;

    private final RestClient cliente;
    private final ConfiguracaoWhatsApp configuracao;

    public WhatsAppCloudApiAdapter(RestClient.Builder construtor, ConfiguracaoWhatsApp configuracao) {
        this.configuracao = configuracao;
        this.cliente = construtor.clone()
                .baseUrl(configuracao.urlBase())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + configuracao.tokenAcesso())
                .build();
    }

    @Override
    public String enviarTexto(WaId destino, String texto) {
        String corpo = texto.length() > LIMITE_TEXTO ? texto.substring(0, LIMITE_TEXTO - 1) + "…" : texto;
        return enviar(EnvioMensagem.texto(destino.valor(), corpo));
    }

    @Override
    public String enviarTemplate(WaId destino, String nomeTemplate, String idioma, List<String> parametrosCorpo) {
        List<Parametro> parametros = parametrosCorpo.stream()
                .map(WhatsAppCloudApiAdapter::parametroTemplate)
                .map(valor -> new Parametro("text", valor))
                .toList();
        List<Componente> componentes = parametros.isEmpty() ? List.of() : List.of(new Componente("body", parametros));
        return enviar(EnvioMensagem.template(destino.valor(), new Template(nomeTemplate, new Idioma(idioma), componentes)));
    }

    @Override
    public MidiaBaixada baixar(String midiaId) {
        exigirConfiguracao();
        try {
            InfoMidia info = cliente.get()
                    .uri("/{versao}/{midia}", configuracao.versaoApi(), midiaId)
                    .retrieve()
                    .body(InfoMidia.class);
            if (info == null || info.url() == null || !info.url().startsWith("https://")) {
                throw new FalhaIntegracaoException("A Cloud API não informou a URL da mídia " + midiaId + ".");
            }
            if (info.tamanho() != null && info.tamanho() > configuracao.tamanhoMaximoMidia()) {
                throw new FalhaIntegracaoException("Mídia " + midiaId + " maior que o limite configurado.");
            }
            ResponseEntity<byte[]> arquivo = cliente.get()
                    .uri(URI.create(info.url()))
                    .retrieve()
                    .toEntity(byte[].class);
            byte[] conteudo = arquivo.getBody();
            if (conteudo == null || conteudo.length == 0) {
                throw new FalhaIntegracaoException("A mídia " + midiaId + " veio vazia.");
            }
            if (conteudo.length > configuracao.tamanhoMaximoMidia()) {
                throw new FalhaIntegracaoException("Mídia " + midiaId + " maior que o limite configurado.");
            }
            String tipo = Optional.ofNullable(info.mimeType())
                    .or(() -> Optional.ofNullable(arquivo.getHeaders().getContentType()).map(MediaType::toString))
                    .orElse("application/octet-stream");
            return new MidiaBaixada(conteudo, tipo);
        } catch (RestClientResponseException e) {
            throw new FalhaIntegracaoException("Cloud API respondeu " + e.getStatusCode().value()
                    + " ao baixar a mídia: " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new FalhaIntegracaoException("Falha de comunicação com a Cloud API ao baixar a mídia.", e);
        }
    }

    private String enviar(EnvioMensagem mensagem) {
        exigirConfiguracao();
        try {
            RespostaEnvio resposta = cliente.post()
                    .uri("/{versao}/{numero}/messages", configuracao.versaoApi(), configuracao.phoneNumberId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mensagem)
                    .retrieve()
                    .body(RespostaEnvio.class);
            return Optional.ofNullable(resposta)
                    .map(RespostaEnvio::messages)
                    .filter(lista -> !lista.isEmpty())
                    .map(List::getFirst)
                    .map(MensagemEnviada::id)
                    .orElseThrow(() -> new FalhaIntegracaoException("A Cloud API não devolveu o id da mensagem."));
        } catch (RestClientResponseException e) {
            throw new FalhaIntegracaoException("Cloud API respondeu " + e.getStatusCode().value()
                    + " ao enviar mensagem: " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new FalhaIntegracaoException("Falha de comunicação com a Cloud API ao enviar mensagem.", e);
        }
    }

    private void exigirConfiguracao() {
        if (!configuracao.configurado()) {
            throw new FalhaIntegracaoException(
                    "WhatsApp não configurado: defina WHATSAPP_PHONE_NUMBER_ID e WHATSAPP_ACCESS_TOKEN.");
        }
    }

    /** Parâmetros de template não aceitam quebras de linha, tabs nem mais de 4 espaços seguidos. */
    static String parametroTemplate(String valor) {
        String limpo = (valor == null || valor.isBlank() ? "-" : valor)
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll(" {4,}", "   ")
                .strip();
        return limpo.length() > LIMITE_PARAMETRO_TEMPLATE ? limpo.substring(0, LIMITE_PARAMETRO_TEMPLATE) : limpo;
    }
}
