package br.com.consultorio.odonto.aplicacao.config;

import br.com.consultorio.odonto.dominio.modelo.EtapaConversa;
import br.com.consultorio.odonto.dominio.modelo.RespostaAgente;
import br.com.consultorio.odonto.dominio.modelo.TipoResposta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Textos fixos enviados ao paciente, com os marcadores NOME_CONSULTORIO, NOME_PACIENTE, DATA_HORA,
 * ENDERECO, PRONTUARIO e DICA (a dica da etapa, usada na mensagem "não entendi").
 */
public record MensagensConversa(String nomeConsultorio, String endereco, Map<TipoResposta, String> textos,
                                Map<EtapaConversa, String> dicas) {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter FORMATO_DATA_HORA =
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy 'às' HH:mm", PT_BR);

    /** Etapas em que o paciente responde algo e, portanto, precisam de dica. */
    public static final Set<EtapaConversa> ETAPAS_COM_DICA = EnumSet.of(
            EtapaConversa.AGUARDANDO_RG, EtapaConversa.AGUARDANDO_NOME, EtapaConversa.AGUARDANDO_DATA_NASCIMENTO,
            EtapaConversa.AGUARDANDO_RESPONSAVEL, EtapaConversa.AGUARDANDO_CONSENTIMENTO,
            EtapaConversa.AGUARDANDO_CONFIRMACAO_NOVA_CONSULTA, EtapaConversa.AGUARDANDO_PROBLEMA,
            EtapaConversa.AGUARDANDO_DATA_HORA, EtapaConversa.AGUARDANDO_CONFIRMACAO_SUGESTAO);

    public MensagensConversa {
        Objects.requireNonNull(nomeConsultorio, "nomeConsultorio");
        Objects.requireNonNull(endereco, "endereco");
        textos = Map.copyOf(new EnumMap<>(textos));
        dicas = Map.copyOf(dicas.isEmpty() ? Map.of() : new EnumMap<>(dicas));
        Set<TipoResposta> faltando = EnumSet.allOf(TipoResposta.class);
        faltando.removeAll(textos.keySet());
        if (!faltando.isEmpty()) {
            throw new IllegalArgumentException("Mensagens da conversa não configuradas: " + faltando);
        }
        Set<EtapaConversa> semDica = EnumSet.copyOf(ETAPAS_COM_DICA);
        semDica.removeAll(dicas.keySet());
        if (!semDica.isEmpty()) {
            throw new IllegalArgumentException("Dicas da conversa não configuradas: " + semDica);
        }
    }

    /** Monta o texto final da resposta, trocando os marcadores pelos valores. */
    public String renderizar(RespostaAgente resposta) {
        String dica = resposta.etapa().map(dicas::get).orElse("");
        return textos.get(resposta.tipo())
                .replace("NOME_CONSULTORIO", nomeConsultorio)
                .replace("NOME_PACIENTE", resposta.nomePaciente().orElse(""))
                .replace("DATA_HORA", resposta.dataHora().map(MensagensConversa::formatarDataHora).orElse(""))
                .replace("ENDERECO", endereco)
                .replace("PRONTUARIO", resposta.prontuario().orElse(""))
                .replace("DICA", dica)
                .replace(" ,", ",")
                .strip();
    }

    /** Ex.: "quarta-feira, 18/11/2026 às 10:00". */
    public static String formatarDataHora(LocalDateTime dataHora) {
        return FORMATO_DATA_HORA.format(dataHora);
    }
}
