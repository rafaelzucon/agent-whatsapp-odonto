package br.com.consultorio.odonto.dominio.modelo;

/**
 * Comportamento de uma etapa da conversa (padrão State): interpreta a resposta do paciente naquela etapa e
 * decide a próxima {@link AcaoConversa}. Cada etapa tem a sua implementação, registrada no {@link FluxoConversa};
 * uma etapa nova entra como uma classe nova, sem alterar a {@link Conversa} (aberto/fechado).
 * <p>
 * Uma {@link br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException} lançada pelo tratador é tratada
 * pela conversa como resposta inválida (conta uma tentativa).
 */
interface TratadorEtapa {

    /**
     * @param resposta texto do paciente, já sem espaços nas pontas (nunca {@code null})
     * @param contexto visão restrita da conversa que o tratador pode ler e alterar
     */
    AcaoConversa tratar(String resposta, ContextoEtapa contexto);
}
