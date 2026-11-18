package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.servico.ValidadorDados;

import java.util.Optional;

/** {@link EtapaConversa#AGUARDANDO_CONFIRMACAO_NOVA_CONSULTA}: o paciente com consulta futura quer outra? */
final class TratadorNovaConsulta implements TratadorEtapa {

    private final ValidadorDados validador;

    TratadorNovaConsulta(ValidadorDados validador) {
        this.validador = validador;
    }

    @Override
    public AcaoConversa tratar(String resposta, ContextoEtapa contexto) {
        Optional<Boolean> querOutra = validador.interpretarSimNao(resposta);
        if (querOutra.isEmpty()) {
            return contexto.respostaInvalida();
        }
        DadosConversa dados = contexto.dados();
        if (querOutra.get()) {
            contexto.avancarPara(EtapaConversa.AGUARDANDO_PROBLEMA);
            return AcaoConversa.responder(RespostaAgente.de(TipoResposta.PEDIR_PROBLEMA).comNome(dados.nomePaciente()));
        }
        contexto.avancarPara(EtapaConversa.CONCLUIDA);
        RespostaAgente encerramento = RespostaAgente.de(TipoResposta.ENCERRAMENTO).comNome(dados.nomePaciente());
        return AcaoConversa.responder(dados.consultaExistente().map(encerramento::comDataHora).orElse(encerramento));
    }
}
