package br.com.consultorio.odonto.infraestrutura.config;

import br.com.consultorio.odonto.aplicacao.casodeuso.AgendarConsultaService;
import br.com.consultorio.odonto.aplicacao.casodeuso.CadastrarPacienteService;
import br.com.consultorio.odonto.aplicacao.casodeuso.ConduzirConversaService;
import br.com.consultorio.odonto.aplicacao.casodeuso.ConsultarCatalogoService;
import br.com.consultorio.odonto.aplicacao.casodeuso.ConsultarPacientesService;
import br.com.consultorio.odonto.aplicacao.casodeuso.ExpirarConversasService;
import br.com.consultorio.odonto.aplicacao.casodeuso.ExpurgarMensagensService;
import br.com.consultorio.odonto.aplicacao.casodeuso.GerenciarAgendaService;
import br.com.consultorio.odonto.aplicacao.casodeuso.IdentificarPacienteService;
import br.com.consultorio.odonto.aplicacao.casodeuso.MontadorVisoes;
import br.com.consultorio.odonto.aplicacao.casodeuso.NotificarConsultorioService;
import br.com.consultorio.odonto.aplicacao.casodeuso.ProcessadorComandosConversa;
import br.com.consultorio.odonto.aplicacao.casodeuso.ReceberMensagensService;
import br.com.consultorio.odonto.aplicacao.casodeuso.ReclassificarQueixaService;
import br.com.consultorio.odonto.aplicacao.casodeuso.RegistrarQueixaService;
import br.com.consultorio.odonto.aplicacao.casodeuso.RespondedorPaciente;
import br.com.consultorio.odonto.aplicacao.casodeuso.TranscreverAudioService;
import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoAgenda;
import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoConversa;
import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoNotificacao;
import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoRetencao;
import br.com.consultorio.odonto.aplicacao.config.MensagensConversa;
import br.com.consultorio.odonto.aplicacao.porta.entrada.AgendarConsultaUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.CadastrarPacienteUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ConduzirConversaUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ConsultarCatalogoUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ConsultarPacientesUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ExpirarConversasUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ExpurgarMensagensUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.GerenciarAgendaUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.IdentificarPacienteUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.NotificarConsultorioUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ReceberMensagensUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ReclassificarQueixaUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.RegistrarQueixaUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.TranscreverAudioUseCase;
import br.com.consultorio.odonto.aplicacao.porta.saida.BloqueioAgendaRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.CanalNotificacaoConsultorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.CatalogoProblemasRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.CifradorDocumento;
import br.com.consultorio.odonto.aplicacao.porta.saida.ClassificadorProblemaIA;
import br.com.consultorio.odonto.aplicacao.porta.saida.ConsultaRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ConversaRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.EnviadorWhatsApp;
import br.com.consultorio.odonto.aplicacao.porta.saida.ExecutorTransacional;
import br.com.consultorio.odonto.aplicacao.porta.saida.InterpretadorDataHoraIA;
import br.com.consultorio.odonto.aplicacao.porta.saida.MensagemRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.PacienteRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ProntuarioRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ProvedorMidia;
import br.com.consultorio.odonto.aplicacao.porta.saida.PublicadorEventos;
import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import br.com.consultorio.odonto.aplicacao.porta.saida.TranscritorAudio;
import br.com.consultorio.odonto.dominio.servico.ClassificadorPorPalavraChave;
import br.com.consultorio.odonto.dominio.servico.DetectorSinaisAlarme;
import br.com.consultorio.odonto.dominio.servico.InterpretadorDataHoraDeterministico;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Composição dos casos de uso. Domínio e aplicação não têm anotações de framework: são montados aqui,
 * recebendo os adaptadores pelas portas de saída (inversão de dependência).
 */
@Configuration(proxyBeanMethods = false)
public class CasosDeUsoConfig {

    @Bean
    ReceberMensagensUseCase receberMensagensUseCase(MensagemRepositorio mensagens, PublicadorEventos publicador,
                                                    ConfiguracaoNotificacao configuracao,
                                                    ExecutorTransacional transacao) {
        return new ReceberMensagensService(mensagens, publicador, configuracao, transacao);
    }

    @Bean
    IdentificarPacienteUseCase identificarPacienteUseCase(PacienteRepositorio pacientes, ConsultaRepositorio consultas,
                                                          CifradorDocumento cifrador, ExecutorTransacional transacao,
                                                          Relogio relogio) {
        return new IdentificarPacienteService(pacientes, consultas, cifrador, transacao, relogio);
    }

    @Bean
    CadastrarPacienteUseCase cadastrarPacienteUseCase(PacienteRepositorio pacientes, ProntuarioRepositorio prontuarios,
                                                      CifradorDocumento cifrador, ExecutorTransacional transacao,
                                                      Relogio relogio) {
        return new CadastrarPacienteService(pacientes, prontuarios, cifrador, transacao, relogio);
    }

    @Bean
    RegistrarQueixaUseCase registrarQueixaUseCase(ProntuarioRepositorio prontuarios,
                                                  CatalogoProblemasRepositorio catalogo,
                                                  ClassificadorProblemaIA classificadorIA,
                                                  ConfiguracaoConversa configuracao, ExecutorTransacional transacao,
                                                  Relogio relogio) {
        return new RegistrarQueixaService(prontuarios, catalogo, classificadorIA, new ClassificadorPorPalavraChave(),
                new DetectorSinaisAlarme(), configuracao, transacao, relogio);
    }

    @Bean
    AgendarConsultaUseCase agendarConsultaUseCase(ConsultaRepositorio consultas, BloqueioAgendaRepositorio bloqueios,
                                                  ProntuarioRepositorio prontuarios, PublicadorEventos publicador,
                                                  ConfiguracaoAgenda configuracao, ExecutorTransacional transacao,
                                                  Relogio relogio) {
        return new AgendarConsultaService(consultas, bloqueios, prontuarios, publicador, configuracao, transacao,
                relogio);
    }

    @Bean
    NotificarConsultorioUseCase notificarConsultorioUseCase(List<CanalNotificacaoConsultorio> canais,
                                                            ConsultaRepositorio consultas,
                                                            PacienteRepositorio pacientes,
                                                            ProntuarioRepositorio prontuarios) {
        return new NotificarConsultorioService(canais, consultas, pacientes, prontuarios);
    }

    @Bean
    TranscreverAudioUseCase transcreverAudioUseCase(ProvedorMidia provedorMidia, TranscritorAudio transcritor) {
        return new TranscreverAudioService(provedorMidia, transcritor);
    }

    @Bean
    ConduzirConversaUseCase conduzirConversaUseCase(MensagemRepositorio mensagens, ConversaRepositorio conversas,
                                                    TranscreverAudioUseCase transcrever,
                                                    NotificarConsultorioUseCase notificar,
                                                    IdentificarPacienteUseCase identificar,
                                                    CadastrarPacienteUseCase cadastrar,
                                                    RegistrarQueixaUseCase registrarQueixa,
                                                    AgendarConsultaUseCase agendar,
                                                    InterpretadorDataHoraIA interpretadorIA,
                                                    EnviadorWhatsApp enviador, MensagensConversa textos,
                                                    ConfiguracaoConversa configuracao, Relogio relogio) {
        ProcessadorComandosConversa processador = new ProcessadorComandosConversa(identificar, cadastrar,
                registrarQueixa, agendar, new InterpretadorDataHoraDeterministico(), interpretadorIA, configuracao,
                relogio);
        RespondedorPaciente respondedor = new RespondedorPaciente(enviador, mensagens, textos, relogio);
        return new ConduzirConversaService(mensagens, conversas, transcrever, notificar, processador, respondedor,
                configuracao, relogio);
    }

    @Bean
    MontadorVisoes montadorVisoes(PacienteRepositorio pacientes, ProntuarioRepositorio prontuarios, Relogio relogio) {
        return new MontadorVisoes(pacientes, prontuarios, relogio);
    }

    @Bean
    GerenciarAgendaUseCase gerenciarAgendaUseCase(ConsultaRepositorio consultas, BloqueioAgendaRepositorio bloqueios,
                                                  PacienteRepositorio pacientes, ProntuarioRepositorio prontuarios,
                                                  AgendarConsultaUseCase agendar, MontadorVisoes visoes,
                                                  ConfiguracaoAgenda configuracao, ExecutorTransacional transacao,
                                                  Relogio relogio) {
        return new GerenciarAgendaService(consultas, bloqueios, pacientes, prontuarios, agendar, visoes, configuracao,
                transacao, relogio);
    }

    @Bean
    ConsultarPacientesUseCase consultarPacientesUseCase(PacienteRepositorio pacientes,
                                                        ProntuarioRepositorio prontuarios,
                                                        ConsultaRepositorio consultas, CifradorDocumento cifrador,
                                                        MontadorVisoes visoes) {
        return new ConsultarPacientesService(pacientes, prontuarios, consultas, cifrador, visoes);
    }

    @Bean
    ReclassificarQueixaUseCase reclassificarQueixaUseCase(ProntuarioRepositorio prontuarios,
                                                          CatalogoProblemasRepositorio catalogo,
                                                          ExecutorTransacional transacao) {
        return new ReclassificarQueixaService(prontuarios, catalogo, transacao);
    }

    @Bean
    ConsultarCatalogoUseCase consultarCatalogoUseCase(CatalogoProblemasRepositorio catalogo) {
        return new ConsultarCatalogoService(catalogo);
    }

    @Bean
    ExpirarConversasUseCase expirarConversasUseCase(ConversaRepositorio conversas, ConfiguracaoConversa configuracao,
                                                    Relogio relogio) {
        return new ExpirarConversasService(conversas, configuracao, relogio);
    }

    @Bean
    ExpurgarMensagensUseCase expurgarMensagensUseCase(MensagemRepositorio mensagens, ConversaRepositorio conversas,
                                                      ConfiguracaoRetencao retencao, ExecutorTransacional transacao,
                                                      Relogio relogio) {
        return new ExpurgarMensagensService(mensagens, conversas, retencao, transacao, relogio);
    }
}
