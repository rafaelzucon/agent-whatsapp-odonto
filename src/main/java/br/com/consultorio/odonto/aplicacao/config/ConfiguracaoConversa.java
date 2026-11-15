package br.com.consultorio.odonto.aplicacao.config;

import br.com.consultorio.odonto.dominio.modelo.RegrasConversa;

import java.time.Duration;

/**
 * Configuração do fluxo da conversa e da classificação de queixas.
 *
 * @param expiracao               tempo sem interação para a conversa recomeçar do início
 * @param maxTentativasPorEtapa   respostas inválidas aceitas antes de encaminhar ao atendimento humano
 * @param confirmarSugestao       se o horário sugerido precisa de "SIM" do paciente antes de ser agendado
 * @param confiancaMinima         confiança mínima (0 a 1) para aceitar a classificação da IA
 */
public record ConfiguracaoConversa(Duration expiracao, int maxTentativasPorEtapa, boolean confirmarSugestao,
                                   double confiancaMinima) {

    public ConfiguracaoConversa {
        if (confiancaMinima < 0 || confiancaMinima > 1) {
            throw new IllegalArgumentException("confiancaMinima deve estar entre 0 e 1.");
        }
    }

    public RegrasConversa regras() {
        return new RegrasConversa(expiracao, maxTentativasPorEtapa, confirmarSugestao);
    }
}
