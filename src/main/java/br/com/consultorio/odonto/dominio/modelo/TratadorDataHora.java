package br.com.consultorio.odonto.dominio.modelo;

/** {@link EtapaConversa#AGUARDANDO_DATA_HORA}: pede a interpretação da data e do horário desejados. */
final class TratadorDataHora implements TratadorEtapa {

    @Override
    public AcaoConversa tratar(String resposta, ContextoEtapa contexto) {
        return AcaoConversa.executar(new ComandoConversa.InterpretarDataHora(resposta));
    }
}
