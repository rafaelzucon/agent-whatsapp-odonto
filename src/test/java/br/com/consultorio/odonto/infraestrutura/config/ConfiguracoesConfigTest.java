package br.com.consultorio.odonto.infraestrutura.config;

import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoAgenda;
import br.com.consultorio.odonto.aplicacao.config.MensagensConversa;
import br.com.consultorio.odonto.dominio.modelo.EtapaConversa;
import br.com.consultorio.odonto.dominio.modelo.RespostaAgente;
import br.com.consultorio.odonto.dominio.modelo.TipoResposta;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** Garante que o application.yml publicado monta todas as configurações, sem texto ou dica faltando. */
class ConfiguracoesConfigTest {

    private static final Pattern MARCADOR = Pattern.compile(
            "NOME_CONSULTORIO|NOME_PACIENTE|DATA_HORA|ENDERECO|PRONTUARIO|DICA");

    private final ConfiguracoesConfig config = new ConfiguracoesConfig();

    private OdontoProperties carregar(Map<String, Object> variaveis) throws IOException {
        StandardEnvironment ambiente = new StandardEnvironment();
        ambiente.getPropertySources().addFirst(new MapPropertySource("variaveis", variaveis));
        List<PropertySource<?>> yaml = new YamlPropertySourceLoader()
                .load("application.yml", new ClassPathResource("application.yml"));
        yaml.forEach(ambiente.getPropertySources()::addLast);
        return Binder.get(ambiente).bind("odonto", OdontoProperties.class).get();
    }

    @Test
    void padroesDoYamlMontamAgendaConversaEMensagens() throws IOException {
        OdontoProperties props = carregar(Map.of());

        ConfiguracaoAgenda agenda = config.configuracaoAgenda(props);
        assertThat(agenda.funcionamento().diasAtendimento()).hasSize(5).doesNotContain(DayOfWeek.SATURDAY);
        assertThat(agenda.funcionamento().almocoInicio()).isEqualTo(LocalTime.NOON);
        assertThat(agenda.feriados()).isEmpty();
        assertThat(config.configuracaoConversa(props).maxTentativasPorEtapa()).isEqualTo(3);
        assertThat(config.configuracaoNotificacao(props).numeroNotificacao()).isEmpty();
        assertThat(config.configuracaoNotificacaoEmail(props).destinatarios()).isEmpty();
        assertThat(config.relogio(props).fuso().getId()).isEqualTo("America/Sao_Paulo");

        MensagensConversa mensagens = config.mensagensConversa(props);
        for (TipoResposta tipo : TipoResposta.values()) {
            RespostaAgente resposta = RespostaAgente.de(tipo).comNome(java.util.Optional.of("Ana"))
                    .comDataHora(LocalDateTime.of(2026, 11, 20, 14, 0)).comProntuario("PR-000001")
                    .comEtapa(EtapaConversa.AGUARDANDO_RG);
            assertThat(mensagens.renderizar(resposta)).as(tipo.name()).isNotBlank().doesNotContainPattern(MARCADOR);
        }
        assertThat(mensagens.renderizar(RespostaAgente.de(TipoResposta.AGENDADO)
                .comNome(java.util.Optional.of("Ana")).comDataHora(LocalDateTime.of(2026, 11, 20, 14, 0))))
                .contains("Ana", "sexta-feira, 20/11/2026 às 14:00", "Rua Exemplo");
    }

    @Test
    void variaveisDeAmbienteSobrescrevemOsPadroes() throws IOException {
        OdontoProperties props = carregar(Map.of(
                "AGENDA_FERIADOS", "2026-12-25,2027-01-01",
                "AGENDA_DIAS", "MONDAY,SATURDAY",
                "WHATSAPP_NUMERO_CONSULTORIO", "5511988887777",
                "EMAIL_DESTINATARIOS", "a@consultorio.com.br, b@consultorio.com.br"));

        ConfiguracaoAgenda agenda = config.configuracaoAgenda(props);
        assertThat(agenda.feriados()).hasSize(2);
        assertThat(agenda.funcionamento().diasAtendimento()).containsExactlyInAnyOrder(DayOfWeek.MONDAY,
                DayOfWeek.SATURDAY);
        assertThat(config.configuracaoNotificacao(props).numeroNotificacao()).isPresent();
        assertThat(config.configuracaoNotificacaoEmail(props).destinatarios())
                .containsExactly("a@consultorio.com.br", "b@consultorio.com.br");
    }
}
