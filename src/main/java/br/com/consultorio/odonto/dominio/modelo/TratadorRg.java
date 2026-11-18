package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.servico.ValidadorDados;

/** {@link EtapaConversa#AGUARDANDO_RG}: valida o RG + UF e pede a identificação do paciente. */
final class TratadorRg implements TratadorEtapa {

    private final ValidadorDados validador;

    TratadorRg(ValidadorDados validador) {
        this.validador = validador;
    }

    @Override
    public AcaoConversa tratar(String resposta, ContextoEtapa contexto) {
        DocumentoRg rg = validador.validarRg(resposta);
        contexto.atualizar(contexto.dados().comRg(rg));
        return AcaoConversa.executar(new ComandoConversa.IdentificarPaciente(rg));
    }
}
