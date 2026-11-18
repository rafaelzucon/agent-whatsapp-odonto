package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.servico.ValidadorDados;

/** {@link EtapaConversa#AGUARDANDO_RESPONSAVEL}: nome do responsável legal do paciente menor de idade. */
final class TratadorResponsavel implements TratadorEtapa {

    private final ValidadorDados validador;

    TratadorResponsavel(ValidadorDados validador) {
        this.validador = validador;
    }

    @Override
    public AcaoConversa tratar(String resposta, ContextoEtapa contexto) {
        contexto.atualizar(contexto.dados().comResponsavel(validador.validarNomeCompleto(resposta)));
        contexto.avancarPara(EtapaConversa.AGUARDANDO_CONSENTIMENTO);
        return AcaoConversa.responder(RespostaAgente.de(TipoResposta.PEDIR_CONSENTIMENTO));
    }
}
