package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.servico.ValidadorDados;

import java.time.LocalDate;

/** {@link EtapaConversa#AGUARDANDO_DATA_NASCIMENTO}: menores de idade precisam informar o responsável. */
final class TratadorDataNascimento implements TratadorEtapa {

    private final ValidadorDados validador;

    TratadorDataNascimento(ValidadorDados validador) {
        this.validador = validador;
    }

    @Override
    public AcaoConversa tratar(String resposta, ContextoEtapa contexto) {
        LocalDate nascimento = validador.validarDataNascimento(resposta, contexto.hoje());
        contexto.atualizar(contexto.dados().comDataNascimento(nascimento));
        if (Paciente.idade(nascimento, contexto.hoje()) < Paciente.MAIORIDADE) {
            contexto.avancarPara(EtapaConversa.AGUARDANDO_RESPONSAVEL);
            return AcaoConversa.responder(RespostaAgente.de(TipoResposta.PEDIR_RESPONSAVEL));
        }
        contexto.avancarPara(EtapaConversa.AGUARDANDO_CONSENTIMENTO);
        return AcaoConversa.responder(RespostaAgente.de(TipoResposta.PEDIR_CONSENTIMENTO));
    }
}
