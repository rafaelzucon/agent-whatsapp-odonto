package br.com.consultorio.odonto.aplicacao.porta.entrada;

/** Processa uma mensagem recebida na máquina de estados da conversa e executa a ação resultante. */
public interface ConduzirConversaUseCase {

    /** Deve ser chamado em sequência para o mesmo número de WhatsApp (sem concorrência por número). */
    void conduzir(long mensagemId);
}
