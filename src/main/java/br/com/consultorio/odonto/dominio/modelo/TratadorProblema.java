package br.com.consultorio.odonto.dominio.modelo;

import static br.com.consultorio.odonto.dominio.modelo.ContextoEtapa.exigir;

/** {@link EtapaConversa#AGUARDANDO_PROBLEMA}: pede o registro da queixa no prontuário. */
final class TratadorProblema implements TratadorEtapa {

    static final int TAMANHO_MINIMO_QUEIXA = 3;

    @Override
    public AcaoConversa tratar(String resposta, ContextoEtapa contexto) {
        if (resposta.length() < TAMANHO_MINIMO_QUEIXA) {
            return contexto.respostaInvalida();
        }
        return AcaoConversa.executar(
                new ComandoConversa.RegistrarQueixa(exigir(contexto.dados().pacienteId(), "paciente"), resposta));
    }
}
