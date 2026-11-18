package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.excecao.TransicaoInvalidaException;

import java.time.LocalDate;
import java.util.Optional;

/**
 * O que um {@link TratadorEtapa} enxerga da {@link Conversa}: só as operações de que precisa para decidir a
 * próxima ação (segregação de interface). Identificação, expiração e persistência continuam encapsuladas.
 */
interface ContextoEtapa {

    DadosConversa dados();

    void atualizar(DadosConversa novosDados);

    /** Muda de etapa e zera o contador de respostas inválidas. */
    void avancarPara(EtapaConversa etapa);

    /** Data atual no fuso do consultório (para calcular idade). */
    LocalDate hoje();

    /** Conta uma tentativa inválida: repete a pergunta com a dica ou, no limite, encaminha para a equipe. */
    AcaoConversa respostaInvalida();

    /** Alerta ao consultório com a identificação do paciente e a prioridade já ajustada. */
    AlertaConversa alerta(TipoAlerta tipo, String resumo);

    /** Comando para reservar o horário para o paciente (e a queixa) da conversa. */
    default ComandoConversa.ReservarHorario reservar(Horario horario) {
        DadosConversa dados = dados();
        return new ComandoConversa.ReservarHorario(exigir(dados.pacienteId(), "paciente"), dados.queixaId(), horario);
    }

    static <T> T exigir(Optional<T> valor, String campo) {
        return valor.orElseThrow(() -> new TransicaoInvalidaException("Dado da conversa ausente: " + campo + "."));
    }
}
