package br.com.consultorio.odonto.infraestrutura.config;

import br.com.consultorio.odonto.dominio.modelo.EtapaConversa;
import br.com.consultorio.odonto.dominio.modelo.TipoResposta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Todas as configurações do agente (prefixo {@code odonto}), validadas na inicialização: um valor errado
 * impede a aplicação de subir, com mensagem clara, em vez de falhar no meio de uma conversa.
 */
@Validated
@ConfigurationProperties(prefix = "odonto")
public record OdontoProperties(
        @NotBlank String nomeConsultorio,
        @NotBlank String endereco,
        @NotBlank String fusoHorario,
        @Valid @NotNull WhatsApp whatsapp,
        @Valid @NotNull Agenda agenda,
        @Valid @NotNull Conversa conversa,
        @Valid @NotNull Retencao retencao,
        @Valid @NotNull Criptografia criptografia,
        @Valid @NotNull Admin admin,
        @Valid @NotNull Email email,
        @Valid @NotNull Mensagens mensagens) {

    public record WhatsApp(
            @NotBlank String urlBase,
            @NotBlank @Pattern(regexp = "v\\d+\\.\\d+", message = "use o formato v24.0") String versaoApi,
            String phoneNumberId,
            String tokenAcesso,
            String tokenVerificacao,
            String segredoApp,
            @Pattern(regexp = "|\\d{10,15}", message = "use só dígitos com DDI e DDD, ex.: 5511999990000")
            String numeroNotificacao,
            boolean usarTemplate,
            @NotBlank String templateAgendamento,
            @NotBlank String templateAlerta,
            @NotBlank String idiomaTemplate,
            @Min(1024) long tamanhoMaximoMidiaBytes) {
    }

    public record Agenda(
            @NotEmpty Set<DayOfWeek> diasAtendimento,
            @NotNull LocalTime abertura,
            @NotNull LocalTime fechamento,
            LocalTime almocoInicio,
            LocalTime almocoFim,
            @NotNull Duration duracaoConsulta,
            @NotNull Duration antecedenciaMinima,
            List<LocalDate> feriados,
            @Min(1) int diasBuscaMaxima) {
    }

    public record Conversa(
            @NotNull Duration expiracao,
            @Min(1) int maxTentativasPorEtapa,
            boolean confirmarSugestao,
            @DecimalMin("0.0") @DecimalMax("1.0") double confiancaMinima,
            @NotNull Duration intervaloVerificacaoExpiracao) {
    }

    public record Retencao(@Min(1) int mensagensDias, @Min(1) int conversasEncerradasDias, @NotBlank String cron) {
    }

    public record Criptografia(String chaveDados, String chaveHash) {

        @Override
        public String toString() {
            return "Criptografia[chaves ocultas]";
        }
    }

    public record Admin(String token) {

        @Override
        public String toString() {
            return "Admin[token=***]";
        }
    }

    public record Email(String remetente, List<String> destinatarios) {
    }

    public record Mensagens(@NotEmpty Map<TipoResposta, String> textos, @NotEmpty Map<EtapaConversa, String> dicas) {
    }
}
