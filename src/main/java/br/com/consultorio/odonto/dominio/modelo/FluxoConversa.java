package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.servico.ValidadorDados;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Liga cada etapa em que o paciente responde ao seu {@link TratadorEtapa}. É o único ponto que muda ao criar
 * uma etapa nova; a {@link Conversa} só pede o tratador da etapa atual. Os colaboradores dos tratadores
 * (como o {@link ValidadorDados}) entram pelo construtor.
 */
final class FluxoConversa {

    static final FluxoConversa PADRAO = new FluxoConversa(new ValidadorDados());

    private final Map<EtapaConversa, TratadorEtapa> tratadores;

    FluxoConversa(ValidadorDados validador) {
        Objects.requireNonNull(validador, "validador");
        Map<EtapaConversa, TratadorEtapa> mapa = new EnumMap<>(EtapaConversa.class);
        mapa.put(EtapaConversa.AGUARDANDO_RG, new TratadorRg(validador));
        mapa.put(EtapaConversa.AGUARDANDO_NOME, new TratadorNome(validador));
        mapa.put(EtapaConversa.AGUARDANDO_DATA_NASCIMENTO, new TratadorDataNascimento(validador));
        mapa.put(EtapaConversa.AGUARDANDO_RESPONSAVEL, new TratadorResponsavel(validador));
        mapa.put(EtapaConversa.AGUARDANDO_CONSENTIMENTO, new TratadorConsentimento(validador));
        mapa.put(EtapaConversa.AGUARDANDO_CONFIRMACAO_NOVA_CONSULTA, new TratadorNovaConsulta(validador));
        mapa.put(EtapaConversa.AGUARDANDO_PROBLEMA, new TratadorProblema());
        mapa.put(EtapaConversa.AGUARDANDO_DATA_HORA, new TratadorDataHora());
        mapa.put(EtapaConversa.AGUARDANDO_CONFIRMACAO_SUGESTAO, new TratadorConfirmacaoSugestao(validador));
        Set<EtapaConversa> semTratador = EnumSet.allOf(EtapaConversa.class);
        semTratador.removeIf(EtapaConversa::ehFinal);
        semTratador.removeAll(mapa.keySet());
        if (!semTratador.isEmpty()) {
            throw new IllegalStateException("Etapas da conversa sem tratador: " + semTratador);
        }
        this.tratadores = Map.copyOf(mapa);
    }

    TratadorEtapa tratadorDe(EtapaConversa etapa) {
        TratadorEtapa tratador = tratadores.get(etapa);
        if (tratador == null) {
            throw new IllegalStateException("Etapa final já tratada: " + etapa);
        }
        return tratador;
    }
}
