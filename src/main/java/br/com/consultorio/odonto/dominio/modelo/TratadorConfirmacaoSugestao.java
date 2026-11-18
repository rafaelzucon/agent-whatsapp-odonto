package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.servico.ValidadorDados;

import java.util.Optional;

import static br.com.consultorio.odonto.dominio.modelo.ContextoEtapa.exigir;

/**
 * {@link EtapaConversa#AGUARDANDO_CONFIRMACAO_SUGESTAO}: "SIM" reserva o horário sugerido, "NÃO" volta a
 * perguntar a data e outra resposta é tratada como um novo pedido de data e horário.
 */
final class TratadorConfirmacaoSugestao implements TratadorEtapa {

    private final ValidadorDados validador;

    TratadorConfirmacaoSugestao(ValidadorDados validador) {
        this.validador = validador;
    }

    @Override
    public AcaoConversa tratar(String resposta, ContextoEtapa contexto) {
        Optional<Boolean> confirma = validador.interpretarSimNao(resposta);
        if (confirma.isEmpty()) {
            return AcaoConversa.executar(new ComandoConversa.InterpretarDataHora(resposta));
        }
        if (confirma.get()) {
            return AcaoConversa.executar(
                    contexto.reservar(exigir(contexto.dados().horarioSugerido(), "horário sugerido")));
        }
        contexto.atualizar(contexto.dados().comHorarioSugerido(Optional.empty()));
        contexto.avancarPara(EtapaConversa.AGUARDANDO_DATA_HORA);
        return AcaoConversa.responder(RespostaAgente.de(TipoResposta.PEDIR_DATA_HORA));
    }
}
