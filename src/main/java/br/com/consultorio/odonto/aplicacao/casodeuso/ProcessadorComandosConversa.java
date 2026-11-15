package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.config.ConfiguracaoConversa;
import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;
import br.com.consultorio.odonto.aplicacao.porta.entrada.AgendarConsultaUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.AgendarConsultaUseCase.AgendarConsultaCommand;
import br.com.consultorio.odonto.aplicacao.porta.entrada.AgendarConsultaUseCase.ResultadoAgendamento;
import br.com.consultorio.odonto.aplicacao.porta.entrada.CadastrarPacienteUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.CadastrarPacienteUseCase.CadastrarPacienteCommand;
import br.com.consultorio.odonto.aplicacao.porta.entrada.CadastrarPacienteUseCase.PacienteCadastrado;
import br.com.consultorio.odonto.aplicacao.porta.entrada.IdentificarPacienteUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.RegistrarQueixaUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.RegistrarQueixaUseCase.QueixaRegistrada;
import br.com.consultorio.odonto.aplicacao.porta.saida.InterpretadorDataHoraIA;
import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import br.com.consultorio.odonto.dominio.modelo.AcaoConversa;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa;
import br.com.consultorio.odonto.dominio.modelo.Conversa;
import br.com.consultorio.odonto.dominio.modelo.PreferenciaHorario;
import br.com.consultorio.odonto.dominio.modelo.RegrasConversa;
import br.com.consultorio.odonto.dominio.servico.InterpretadorDataHoraDeterministico;

import java.lang.System.Logger.Level;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Executa os comandos emitidos pela máquina de estados da conversa, delegando aos casos de uso, e devolve o
 * resultado à conversa, que decide a ação seguinte.
 */
public final class ProcessadorComandosConversa {

    private static final System.Logger LOG = System.getLogger(ProcessadorComandosConversa.class.getName());

    private final IdentificarPacienteUseCase identificarPaciente;
    private final CadastrarPacienteUseCase cadastrarPaciente;
    private final RegistrarQueixaUseCase registrarQueixa;
    private final AgendarConsultaUseCase agendarConsulta;
    private final InterpretadorDataHoraDeterministico interpretadorDeterministico;
    private final InterpretadorDataHoraIA interpretadorIA;
    private final ConfiguracaoConversa configuracao;
    private final Relogio relogio;

    public ProcessadorComandosConversa(IdentificarPacienteUseCase identificarPaciente,
                                       CadastrarPacienteUseCase cadastrarPaciente,
                                       RegistrarQueixaUseCase registrarQueixa,
                                       AgendarConsultaUseCase agendarConsulta,
                                       InterpretadorDataHoraDeterministico interpretadorDeterministico,
                                       InterpretadorDataHoraIA interpretadorIA,
                                       ConfiguracaoConversa configuracao, Relogio relogio) {
        this.identificarPaciente = Objects.requireNonNull(identificarPaciente);
        this.cadastrarPaciente = Objects.requireNonNull(cadastrarPaciente);
        this.registrarQueixa = Objects.requireNonNull(registrarQueixa);
        this.agendarConsulta = Objects.requireNonNull(agendarConsulta);
        this.interpretadorDeterministico = Objects.requireNonNull(interpretadorDeterministico);
        this.interpretadorIA = Objects.requireNonNull(interpretadorIA);
        this.configuracao = Objects.requireNonNull(configuracao);
        this.relogio = Objects.requireNonNull(relogio);
    }

    public AcaoConversa executar(Conversa conversa, ComandoConversa comando) {
        RegrasConversa regras = configuracao.regras();
        return switch (comando) {
            case ComandoConversa.IdentificarPaciente(var rg) -> identificarPaciente.identificar(rg, conversa.waId())
                    .map(p -> conversa.pacienteEncontrado(p.pacienteId(), p.primeiroNome(), p.consultaFutura()))
                    .orElseGet(conversa::pacienteNaoEncontrado);
            case ComandoConversa.CadastrarPaciente(var rg, var nome, var nascimento, var responsavel) -> {
                PacienteCadastrado cadastrado = cadastrarPaciente.cadastrar(
                        new CadastrarPacienteCommand(rg, nome, nascimento, responsavel, conversa.waId()));
                yield conversa.pacienteCadastrado(cadastrado.pacienteId(), cadastrado.primeiroNome(),
                        cadastrado.numeroProntuario());
            }
            case ComandoConversa.RegistrarQueixa(var pacienteId, var texto) -> {
                QueixaRegistrada queixa = registrarQueixa.registrar(pacienteId, texto);
                yield conversa.queixaRegistrada(queixa.queixaId(), queixa.nomeProblema(), queixa.prioridade());
            }
            case ComandoConversa.InterpretarDataHora(var texto) -> interpretar(texto)
                    .map(preferencia -> conversa.disponibilidadeVerificada(agendarConsulta.verificar(preferencia), regras))
                    .orElseGet(() -> conversa.dataHoraNaoEntendida(regras));
            case ComandoConversa.BuscarPrimeiroHorarioLivre() ->
                    conversa.disponibilidadeVerificada(agendarConsulta.primeiroHorarioLivre(), regras);
            case ComandoConversa.ReservarHorario(var pacienteId, var queixaId, var horario) -> {
                ResultadoAgendamento resultado = agendarConsulta.agendar(
                        new AgendarConsultaCommand(pacienteId, queixaId, horario.inicio(), true));
                yield switch (resultado) {
                    case ResultadoAgendamento.Agendada agendada -> conversa.consultaAgendada(agendada.horario());
                    case ResultadoAgendamento.Indisponivel indisponivel ->
                            conversa.disponibilidadeVerificada(indisponivel.alternativa(), regras);
                };
            }
        };
    }

    /** Primeiro o interpretador determinístico (rápido e previsível); a IA só entra se ele não entender. */
    Optional<PreferenciaHorario> interpretar(String texto) {
        LocalDateTime agora = relogio.agoraLocal();
        Optional<PreferenciaHorario> deterministica = interpretadorDeterministico.interpretar(texto, agora);
        if (deterministica.isPresent()) {
            return deterministica;
        }
        try {
            return interpretadorIA.interpretar(texto, agora, relogio.fuso());
        } catch (FalhaIntegracaoException e) {
            LOG.log(Level.WARNING, "Interpretação de data por IA indisponível: {0}", e.getMessage());
            return Optional.empty();
        }
    }
}
