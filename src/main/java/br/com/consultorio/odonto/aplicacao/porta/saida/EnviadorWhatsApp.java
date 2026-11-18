package br.com.consultorio.odonto.aplicacao.porta.saida;

/**
 * Envia mensagens pelo número do consultório na WhatsApp Cloud API: texto livre e templates. Quem só precisa de
 * um dos dois depende da interface específica ({@link EnviadorTextoWhatsApp} ou {@link EnviadorTemplateWhatsApp}).
 */
public interface EnviadorWhatsApp extends EnviadorTextoWhatsApp, EnviadorTemplateWhatsApp {
}
