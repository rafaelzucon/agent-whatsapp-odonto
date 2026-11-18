package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.servico.ValidadorDados;

/** {@link EtapaConversa#AGUARDANDO_NOME}: guarda o nome completo do paciente novo. */
final class TratadorNome implements TratadorEtapa {

    private final ValidadorDados validador;

    TratadorNome(ValidadorDados validador) {
        this.validador = validador;
    }

    @Override
    public AcaoConversa tratar(String resposta, ContextoEtapa contexto) {
        contexto.atualizar(contexto.dados().comNome(validador.validarNomeCompleto(resposta)));
        contexto.avancarPara(EtapaConversa.AGUARDANDO_DATA_NASCIMENTO);
        return AcaoConversa.responder(RespostaAgente.de(TipoResposta.PEDIR_DATA_NASCIMENTO));
    }
}
