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
import br.com.consultorio.odonto.aplicacao.casodeuso.NotificacaoMultiCanal;
import br.com.consultorio.odonto.aplicacao.casodeuso.NotificarConsultorioService;
import br.com.consultorio.odonto.aplicacao.casodeuso.ReceberMensagensService;
import br.com.consultorio.odonto.aplicacao.casodeuso.ReclassificarQueixaService;
import br.com.consultorio.odonto.aplicacao.casodeuso.RegistrarQueixaService;
import br.com.consultorio.odonto.aplicacao.casodeuso.RespondedorPacienteWhatsApp;
import br.com.consultorio.odonto.aplicacao.casodeuso.TranscreverAudioService;
import br.com.consultorio.odonto.aplicacao.casodeuso.classificacao.ClassificacaoPorIA;
import br.com.consultorio.odonto.aplicacao.casodeuso.classificacao.ClassificacaoPorPalavraChave;
import br.com.consultorio.odonto.aplicacao.casodeuso.classificacao.ClassificadorQueixaEmCadeia;
import br.com.consultorio.odonto.aplicacao.casodeuso.comando.BuscarPrimeiroHorarioLivreExecutor;
import br.com.consultorio.odonto.aplicacao.casodeuso.comando.CadastrarPacienteExecutor;
import br.com.consultorio.odonto.aplicacao.casodeuso.comando.IdentificarPacienteExecutor;
import br.com.consultorio.odonto.aplicacao.casodeuso.comando.InterpretarDataHoraExecutor;
import br.com.consultorio.odonto.aplicacao.casodeuso.comando.ProcessadorComandosConversa;
import br.com.consultorio.odonto.aplicacao.casodeuso.comando.ProcessadorComandosPorTipo;
import br.com.consultorio.odonto.aplicacao.casodeuso.comando.RegistrarQueixaExecutor;
import br.com.consultorio.odonto.aplicacao.casodeuso.comando.ReservarHorarioExecutor;
import br.com.consultorio.odonto.aplicacao.casodeuso.interpretacao.InterpretacaoDeterministica;
import br.com.consultorio.odonto.aplicacao.casodeuso.interpretacao.InterpretacaoEmCadeia;
import br.com.consultorio.odonto.aplicacao.casodeuso.interpretacao.InterpretacaoPorIA;
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
import br.com.consultorio.odonto.aplicacao.porta.saida.EnviadorTextoWhatsApp;
import br.com.consultorio.odonto.aplicacao.porta.saida.ExecutorTransacional;
import br.com.consultorio.odonto.aplicacao.porta.saida.InterpretadorDataHoraIA;
import br.com.consultorio.odonto.aplicacao.porta.saida.MensagemRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.PacienteRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ProntuarioRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ProvedorMidia;
import br.com.consultorio.odonto.aplicacao.porta.saida.PublicadorEventos;
import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import br.com.consultorio.odonto.aplicacao.porta.saida.RetencaoConversas;
import br.com.consultorio.odonto.aplicacao.porta.saida.RetencaoMensagens;
import br.com.consultorio.odonto.aplicacao.porta.saida.TranscritorAudio;
import br.com.consultorio.odonto.dominio.servico.ClassificadorPorPalavraChave;
import br.com.consultorio.odonto.dominio.servico.DetectorSinaisAlarme;
import br.com.consultorio.odonto.dominio.servico.InterpretadorDataHoraDeterministico;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Composição dos casos de uso (composition root). Domínio e aplicação não têm anotações de framework: são montados
 * aqui, recebendo os adaptadores pelas portas de saída (inversão de dependência). É também aqui que se escolhem e
 * ordenam as estratégias (classificação, interpretação de data, executores de comando, canais de notificação):
 * estender o comportamento é registrar uma implementação nova, sem alterar os casos de uso (aberto/fechado).
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
        ClassificadorQueixaEmCadeia classificador = new ClassificadorQueixaEmCadeia(catalogo, List.of(
                new ClassificacaoPorIA(classificadorIA, configuracao.confiancaMinima()),
                new ClassificacaoPorPalavraChave(new ClassificadorPorPalavraChave())), new DetectorSinaisAlarme());
        return new RegistrarQueixaService(prontuarios, classificador, transacao, relogio);
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
        return new NotificarConsultorioService(new NotificacaoMultiCanal(canais), consultas, pacientes, prontuarios);
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
                                                    EnviadorTextoWhatsApp enviador, MensagensConversa textos,
                                                    ConfiguracaoConversa configuracao, Relogio relogio) {
        // Determinística primeiro (rápida e previsível); a IA só entra se ela não entender.
        InterpretacaoEmCadeia interpretador = new InterpretacaoEmCadeia(List.of(
                new InterpretacaoDeterministica(new InterpretadorDataHoraDeterministico(), relogio),
                new InterpretacaoPorIA(interpretadorIA, relogio)));
        ProcessadorComandosConversa processador = new ProcessadorComandosPorTipo(List.of(
                new IdentificarPacienteExecutor(identificar),
                new CadastrarPacienteExecutor(cadastrar),
                new RegistrarQueixaExecutor(registrarQueixa),
                new InterpretarDataHoraExecutor(interpretador, agendar, configuracao),
                new BuscarPrimeiroHorarioLivreExecutor(agendar, configuracao),
                new ReservarHorarioExecutor(agendar, configuracao)));
        return new ConduzirConversaService(mensagens, conversas, transcrever, notificar, processador,
                new RespondedorPacienteWhatsApp(enviador, mensagens, textos, relogio), configuracao, relogio);
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
    ExpirarConversasUseCase expirarConversasUseCase(RetencaoConversas conversas, ConfiguracaoConversa configuracao,
                                                    Relogio relogio) {
        return new ExpirarConversasService(conversas, configuracao, relogio);
    }

    @Bean
    ExpurgarMensagensUseCase expurgarMensagensUseCase(RetencaoMensagens mensagens, RetencaoConversas conversas,
                                                      ConfiguracaoRetencao retencao, ExecutorTransacional transacao,
                                                      Relogio relogio) {
        return new ExpurgarMensagensService(mensagens, conversas, retencao, transacao, relogio);
    }
}
