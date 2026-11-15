package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.casodeuso.Fakes.BloqueiosEmMemoria;
import br.com.consultorio.odonto.aplicacao.casodeuso.Fakes.CanalFake;
import br.com.consultorio.odonto.aplicacao.casodeuso.Fakes.CatalogoEmMemoria;
import br.com.consultorio.odonto.aplicacao.casodeuso.Fakes.CifradorSimples;
import br.com.consultorio.odonto.aplicacao.casodeuso.Fakes.ClassificadorIAFake;
import br.com.consultorio.odonto.aplicacao.casodeuso.Fakes.ConsultasEmMemoria;
import br.com.consultorio.odonto.aplicacao.casodeuso.Fakes.ConversasEmMemoria;
import br.com.consultorio.odonto.aplicacao.casodeuso.Fakes.EnviadorFake;
import br.com.consultorio.odonto.aplicacao.casodeuso.Fakes.InterpretadorIAFake;
import br.com.consultorio.odonto.aplicacao.casodeuso.Fakes.MensagensEmMemoria;
import br.com.consultorio.odonto.aplicacao.casodeuso.Fakes.MidiaFake;
import br.com.consultorio.odonto.aplicacao.casodeuso.Fakes.PacientesEmMemoria;
import br.com.consultorio.odonto.aplicacao.casodeuso.Fakes.ProntuariosEmMemoria;
import br.com.consultorio.odonto.aplicacao.casodeuso.Fakes.PublicadorFake;
import br.com.consultorio.odonto.aplicacao.casodeuso.Fakes.RelogioFixo;
import br.com.consultorio.odonto.aplicacao.casodeuso.Fakes.TransacaoDireta;
import br.com.consultorio.odonto.aplicacao.casodeuso.Fakes.TranscritorFake;
import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoAgenda;
import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoConversa;
import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoNotificacao;
import br.com.consultorio.odonto.aplicacao.config.MensagensConversa;
import br.com.consultorio.odonto.aplicacao.evento.ConsultaAgendadaEvento;
import br.com.consultorio.odonto.aplicacao.evento.MensagemRecebidaEvento;
import br.com.consultorio.odonto.aplicacao.porta.entrada.MensagemRecebidaCommand;
import br.com.consultorio.odonto.dominio.modelo.EtapaConversa;
import br.com.consultorio.odonto.dominio.modelo.HorarioFuncionamento;
import br.com.consultorio.odonto.dominio.modelo.TipoResposta;
import br.com.consultorio.odonto.dominio.modelo.WaId;
import br.com.consultorio.odonto.dominio.servico.ClassificadorPorPalavraChave;
import br.com.consultorio.odonto.dominio.servico.DetectorSinaisAlarme;
import br.com.consultorio.odonto.dominio.servico.InterpretadorDataHoraDeterministico;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monta todos os casos de uso com as portas em memória, do mesmo jeito que a configuração do Spring.
 * Os textos configurados são "TIPO-RESPOSTA | nome | data | prontuário | dica" para facilitar as asserções.
 */
final class Cenario {

    static final String PACIENTE = "5511999990001";
    static final String CONSULTORIO = "5511888880000";

    static final HorarioFuncionamento EXPEDIENTE = new HorarioFuncionamento(
            EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), LocalTime.of(8, 0), LocalTime.of(18, 0),
            LocalTime.of(12, 0), LocalTime.of(13, 0), Duration.ofMinutes(30), Duration.ofHours(2));

    final RelogioFixo relogio = new RelogioFixo();
    final TransacaoDireta transacao = new TransacaoDireta();
    final CifradorSimples cifrador = new CifradorSimples();
    final PacientesEmMemoria pacientes = new PacientesEmMemoria();
    final ProntuariosEmMemoria prontuarios = new ProntuariosEmMemoria();
    final ConsultasEmMemoria consultas = new ConsultasEmMemoria();
    final BloqueiosEmMemoria bloqueios = new BloqueiosEmMemoria();
    final ConversasEmMemoria conversas = new ConversasEmMemoria();
    final MensagensEmMemoria mensagens = new MensagensEmMemoria();
    final CatalogoEmMemoria catalogo = new CatalogoEmMemoria();
    final ClassificadorIAFake classificadorIA = new ClassificadorIAFake();
    final InterpretadorIAFake interpretadorIA = new InterpretadorIAFake();
    final EnviadorFake enviador = new EnviadorFake();
    final CanalFake canalWhatsApp = new CanalFake("whatsapp");
    final CanalFake canalEmail = new CanalFake("email");
    final PublicadorFake publicador = new PublicadorFake();
    final TranscritorFake transcritor = new TranscritorFake();

    final ConfiguracaoConversa configuracaoConversa = new ConfiguracaoConversa(Duration.ofHours(2), 3, true, 0.6);
    final ConfiguracaoAgenda configuracaoAgenda = new ConfiguracaoAgenda(EXPEDIENTE, Set.of(), 60);
    final MensagensConversa textos = new MensagensConversa("Odonto Teste", "Rua A, 1",
            Arrays.stream(TipoResposta.values()).collect(Collectors.toMap(Function.identity(),
                    t -> t.name().replace('_', '-') + " | NOME_PACIENTE | DATA_HORA | PRONTUARIO | DICA")),
            MensagensConversa.ETAPAS_COM_DICA.stream().collect(Collectors.toMap(Function.identity(),
                    e -> "dica " + e.name())));

    final ReceberMensagensService receber = new ReceberMensagensService(mensagens, publicador,
            new ConfiguracaoNotificacao(Optional.of(new WaId(CONSULTORIO))), transacao);
    final IdentificarPacienteService identificar = new IdentificarPacienteService(pacientes, consultas, cifrador,
            transacao, relogio);
    final CadastrarPacienteService cadastrar = new CadastrarPacienteService(pacientes, prontuarios, cifrador,
            transacao, relogio);
    final RegistrarQueixaService registrarQueixa = new RegistrarQueixaService(prontuarios, catalogo, classificadorIA,
            new ClassificadorPorPalavraChave(), new DetectorSinaisAlarme(), configuracaoConversa, transacao, relogio);
    final AgendarConsultaService agendar = new AgendarConsultaService(consultas, bloqueios, prontuarios, publicador,
            configuracaoAgenda, transacao, relogio);
    final NotificarConsultorioService notificar = new NotificarConsultorioService(
            List.of(canalWhatsApp, canalEmail), consultas, pacientes, prontuarios);
    final TranscreverAudioService transcrever = new TranscreverAudioService(new MidiaFake(), transcritor);
    final ConduzirConversaService conduzir = new ConduzirConversaService(mensagens, conversas, transcrever, notificar,
            new ProcessadorComandosConversa(identificar, cadastrar, registrarQueixa, agendar,
                    new InterpretadorDataHoraDeterministico(), interpretadorIA, configuracaoConversa, relogio),
            new RespondedorPaciente(enviador, mensagens, textos, relogio), configuracaoConversa, relogio);
    final MontadorVisoes visoes = new MontadorVisoes(pacientes, prontuarios, relogio);

    private int sequenciaMensagens;

    /** Simula o webhook + o listener assíncrono: recebe a mensagem e conduz a conversa. */
    List<String> paciente(String texto) {
        return processar(MensagemRecebidaCommand.texto(proximoWamid(), PACIENTE, texto, relogio.agora()));
    }

    List<String> processar(MensagemRecebidaCommand comando) {
        int antes = enviador.textos.size();
        receber.receber(List.of(comando));
        List<MensagemRecebidaEvento> eventos = publicador.eventos(MensagemRecebidaEvento.class);
        conduzir.conduzir(eventos.getLast().mensagemId());
        publicador.eventos(ConsultaAgendadaEvento.class).stream()
                .filter(e -> !avisados.contains(e.consultaId()))
                .forEach(e -> {
                    avisados.add(e.consultaId());
                    notificar.notificarAgendamento(e.consultaId());
                });
        return List.copyOf(enviador.textos.subList(antes, enviador.textos.size()));
    }

    private final Set<Long> avisados = new HashSet<>();

    String proximoWamid() {
        return "wamid.recebida." + (++sequenciaMensagens);
    }

    EtapaConversa etapa() {
        return conversas.buscarPorWaId(new WaId(PACIENTE)).orElseThrow().etapa();
    }

    /** Conduz um paciente novo até a pergunta do problema. */
    void cadastrarPacienteNovo() {
        paciente("Olá");
        paciente("12.345.678-9 SP");
        paciente("maria da silva");
        paciente("10/05/1990");
        paciente("sim");
    }
}
