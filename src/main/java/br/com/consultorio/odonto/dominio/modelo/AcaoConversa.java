package br.com.consultorio.odonto.dominio.modelo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Próxima ação decidida pela máquina de estados: mensagens a enviar ao paciente, um alerta opcional
 * para o consultório e um comando opcional a executar (cadastro, agendamento, etc.).
 */
public record AcaoConversa(List<RespostaAgente> respostas, Optional<AlertaConversa> alerta,
                           Optional<ComandoConversa> comando) {

    public AcaoConversa {
        respostas = List.copyOf(Objects.requireNonNull(respostas, "respostas"));
        Objects.requireNonNull(alerta, "alerta");
        Objects.requireNonNull(comando, "comando");
    }

    public static AcaoConversa responder(RespostaAgente... respostas) {
        return new AcaoConversa(List.of(respostas), Optional.empty(), Optional.empty());
    }

    public static AcaoConversa executar(ComandoConversa comando) {
        return new AcaoConversa(List.of(), Optional.empty(), Optional.of(comando));
    }

    public static AcaoConversa silencio() {
        return new AcaoConversa(List.of(), Optional.empty(), Optional.empty());
    }

    public AcaoConversa comAlerta(AlertaConversa novoAlerta) {
        return new AcaoConversa(respostas, Optional.of(novoAlerta), comando);
    }

    public AcaoConversa comComando(ComandoConversa novoComando) {
        return new AcaoConversa(respostas, alerta, Optional.of(novoComando));
    }
}
