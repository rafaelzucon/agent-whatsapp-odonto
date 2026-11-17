package br.com.consultorio.odonto.infraestrutura.config;

import br.com.consultorio.odonto.adaptador.entrada.web.ConfiguracaoAdmin;
import br.com.consultorio.odonto.adaptador.entrada.webhook.ConfiguracaoWebhook;
import br.com.consultorio.odonto.adaptador.saida.notificacao.ConfiguracaoNotificacaoEmail;
import br.com.consultorio.odonto.adaptador.saida.notificacao.ConfiguracaoNotificacaoWhatsApp;
import br.com.consultorio.odonto.adaptador.saida.persistencia.cripto.ConfiguracaoCriptografia;
import br.com.consultorio.odonto.adaptador.saida.plataforma.RelogioSistema;
import br.com.consultorio.odonto.adaptador.saida.whatsapp.ConfiguracaoWhatsApp;
import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoAgenda;
import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoConversa;
import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoNotificacao;
import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoRetencao;
import br.com.consultorio.odonto.aplicacao.config.MensagensConversa;
import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import br.com.consultorio.odonto.dominio.modelo.HorarioFuncionamento;
import br.com.consultorio.odonto.dominio.modelo.WaId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Converte {@link OdontoProperties} nos objetos de configuração de cada camada. Assim, aplicação e
 * adaptadores recebem records simples e não conhecem o mecanismo de propriedades do Spring.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracoesConfig {

    @Bean
    Relogio relogio(OdontoProperties props) {
        return new RelogioSistema(ZoneId.of(props.fusoHorario()));
    }

    @Bean
    ConfiguracaoAgenda configuracaoAgenda(OdontoProperties props) {
        OdontoProperties.Agenda agenda = props.agenda();
        HorarioFuncionamento funcionamento = new HorarioFuncionamento(agenda.diasAtendimento(), agenda.abertura(),
                agenda.fechamento(), agenda.almocoInicio(), agenda.almocoFim(), agenda.duracaoConsulta(),
                agenda.antecedenciaMinima());
        Set<java.time.LocalDate> feriados = agenda.feriados() == null ? Set.of() : Set.copyOf(agenda.feriados());
        return new ConfiguracaoAgenda(funcionamento, feriados, agenda.diasBuscaMaxima());
    }

    @Bean
    ConfiguracaoConversa configuracaoConversa(OdontoProperties props) {
        OdontoProperties.Conversa conversa = props.conversa();
        return new ConfiguracaoConversa(conversa.expiracao(), conversa.maxTentativasPorEtapa(),
                conversa.confirmarSugestao(), conversa.confiancaMinima());
    }

    @Bean
    ConfiguracaoRetencao configuracaoRetencao(OdontoProperties props) {
        return new ConfiguracaoRetencao(props.retencao().mensagensDias(), props.retencao().conversasEncerradasDias());
    }

    @Bean
    ConfiguracaoNotificacao configuracaoNotificacao(OdontoProperties props) {
        String numero = props.whatsapp().numeroNotificacao();
        return new ConfiguracaoNotificacao(numero == null || numero.isBlank()
                ? Optional.empty() : Optional.of(new WaId(numero.strip())));
    }

    @Bean
    MensagensConversa mensagensConversa(OdontoProperties props) {
        return new MensagensConversa(props.nomeConsultorio(), props.endereco(), props.mensagens().textos(),
                props.mensagens().dicas());
    }

    @Bean
    ConfiguracaoCriptografia configuracaoCriptografia(OdontoProperties props) {
        return new ConfiguracaoCriptografia(props.criptografia().chaveDados(), props.criptografia().chaveHash());
    }

    @Bean
    ConfiguracaoWhatsApp configuracaoWhatsApp(OdontoProperties props) {
        OdontoProperties.WhatsApp whatsapp = props.whatsapp();
        return new ConfiguracaoWhatsApp(whatsapp.urlBase(), whatsapp.versaoApi(), whatsapp.phoneNumberId(),
                whatsapp.tokenAcesso(), whatsapp.tamanhoMaximoMidiaBytes());
    }

    @Bean
    ConfiguracaoNotificacaoWhatsApp configuracaoNotificacaoWhatsApp(OdontoProperties props) {
        OdontoProperties.WhatsApp whatsapp = props.whatsapp();
        return new ConfiguracaoNotificacaoWhatsApp(whatsapp.usarTemplate(), whatsapp.templateAgendamento(),
                whatsapp.templateAlerta(), whatsapp.idiomaTemplate());
    }

    @Bean
    ConfiguracaoWebhook configuracaoWebhook(OdontoProperties props) {
        OdontoProperties.WhatsApp whatsapp = props.whatsapp();
        return new ConfiguracaoWebhook(whatsapp.tokenVerificacao(), whatsapp.segredoApp(), whatsapp.phoneNumberId());
    }

    @Bean
    ConfiguracaoNotificacaoEmail configuracaoNotificacaoEmail(OdontoProperties props) {
        List<String> destinatarios = props.email().destinatarios() == null
                ? List.of()
                : props.email().destinatarios().stream().map(String::strip).filter(s -> !s.isEmpty()).toList();
        return new ConfiguracaoNotificacaoEmail(props.email().remetente(), destinatarios);
    }

    @Bean
    ConfiguracaoAdmin configuracaoAdmin(OdontoProperties props) {
        return new ConfiguracaoAdmin(props.admin().token());
    }
}
