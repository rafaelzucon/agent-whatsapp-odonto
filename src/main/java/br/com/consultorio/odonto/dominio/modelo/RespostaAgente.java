package br.com.consultorio.odonto.dominio.modelo;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Mensagem que o agente deve enviar, com os valores dos placeholders.
 *
 * @param etapa etapa a que se refere a dica (para {@link TipoResposta#NAO_ENTENDI})
 */
public record RespostaAgente(TipoResposta tipo, Optional<String> nomePaciente, Optional<LocalDateTime> dataHora,
                             Optional<String> prontuario, Optional<EtapaConversa> etapa) {

    public RespostaAgente {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(nomePaciente, "nomePaciente");
        Objects.requireNonNull(dataHora, "dataHora");
        Objects.requireNonNull(prontuario, "prontuario");
        Objects.requireNonNull(etapa, "etapa");
    }

    public static RespostaAgente de(TipoResposta tipo) {
        return new RespostaAgente(tipo, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public RespostaAgente comNome(Optional<String> nome) {
        return new RespostaAgente(tipo, nome, dataHora, prontuario, etapa);
    }

    public RespostaAgente comDataHora(LocalDateTime valor) {
        return new RespostaAgente(tipo, nomePaciente, Optional.of(valor), prontuario, etapa);
    }

    public RespostaAgente comProntuario(String numero) {
        return new RespostaAgente(tipo, nomePaciente, dataHora, Optional.of(numero), etapa);
    }

    public RespostaAgente comEtapa(EtapaConversa valor) {
        return new RespostaAgente(tipo, nomePaciente, dataHora, prontuario, Optional.of(valor));
    }
}
