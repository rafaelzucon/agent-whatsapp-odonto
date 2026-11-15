package br.com.consultorio.odonto.aplicacao.porta.saida;

import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;

/**
 * Um canal de aviso ao consultório (WhatsApp, e-mail, log...). Novos canais entram como novas
 * implementações, sem alterar os casos de uso (aberto/fechado).
 */
public interface CanalNotificacaoConsultorio {

    /** Nome curto do canal, para logs. */
    String nome();

    /** {@code false} se o canal não está configurado; nesse caso não é chamado. */
    boolean habilitado();

    /** @throws FalhaIntegracaoException se o envio falhar (os demais canais continuam) */
    void notificarAgendamento(AvisoAgendamento aviso);

    /** @throws FalhaIntegracaoException se o envio falhar (os demais canais continuam) */
    void notificarAlerta(AlertaConsultorio alerta);
}
