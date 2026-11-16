package br.com.consultorio.odonto.adaptador.saida.notificacao;

import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;
import br.com.consultorio.odonto.aplicacao.porta.saida.AlertaConsultorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.AvisoAgendamento;
import br.com.consultorio.odonto.aplicacao.porta.saida.CanalNotificacaoConsultorio;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Avisa o consultório por e-mail, quando o SMTP e os destinatários estão configurados. */
@Component
@Order(2)
public class EmailCanalNotificacao implements CanalNotificacaoConsultorio {

    private final ObjectProvider<JavaMailSender> remetenteEmail;
    private final ConfiguracaoNotificacaoEmail configuracao;

    public EmailCanalNotificacao(ObjectProvider<JavaMailSender> remetenteEmail,
                                 ConfiguracaoNotificacaoEmail configuracao) {
        this.remetenteEmail = remetenteEmail;
        this.configuracao = configuracao;
    }

    @Override
    public String nome() {
        return "email";
    }

    @Override
    public boolean habilitado() {
        return !configuracao.destinatarios().isEmpty() && remetenteEmail.getIfAvailable() != null;
    }

    @Override
    public void notificarAgendamento(AvisoAgendamento aviso) {
        enviar("Nova consulta: " + aviso.nomePaciente() + " em " + aviso.dataHoraFormatada(), aviso.texto());
    }

    @Override
    public void notificarAlerta(AlertaConsultorio alerta) {
        enviar("Alerta do agente de WhatsApp: " + alerta.tipo().descricao(), alerta.texto());
    }

    private void enviar(String assunto, String corpo) {
        JavaMailSender email = remetenteEmail.getIfAvailable();
        if (email == null) {
            throw new FalhaIntegracaoException("SMTP não configurado.");
        }
        SimpleMailMessage mensagem = new SimpleMailMessage();
        if (!configuracao.remetente().isEmpty()) {
            mensagem.setFrom(configuracao.remetente());
        }
        mensagem.setTo(configuracao.destinatarios().toArray(String[]::new));
        mensagem.setSubject(assunto);
        mensagem.setText(corpo);
        try {
            email.send(mensagem);
        } catch (MailException e) {
            throw new FalhaIntegracaoException("Falha ao enviar e-mail ao consultório: " + e.getMessage(), e);
        }
    }
}
