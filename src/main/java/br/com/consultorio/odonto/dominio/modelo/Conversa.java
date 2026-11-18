package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;
import br.com.consultorio.odonto.dominio.excecao.TransicaoInvalidaException;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa.BuscarPrimeiroHorarioLivre;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa.IdentificarPaciente;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa.InterpretarDataHora;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa.RegistrarQueixa;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa.ReservarHorario;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Conversa de WhatsApp com um número (uma por número). Contém a máquina de estados do atendimento:
 * cada resposta do paciente é registrada em {@link #registrarResposta} e a conversa devolve a próxima
 * {@link AcaoConversa}. A interpretação da resposta em cada etapa é delegada ao {@link TratadorEtapa} da etapa
 * (padrão State, registrado no {@link FluxoConversa}); aqui ficam o ciclo de vida, as transições disparadas
 * pelos resultados dos comandos e as regras comuns (tentativas, alertas, encaminhamento à equipe). Quando a ação traz um comando, a camada de aplicação o executa e informa o
 * resultado pelo método correspondente (ex.: {@link #pacienteEncontrado}), que devolve a ação seguinte.
 */
public final class Conversa {

    private final Long id;
    private final WaId waId;
    private EtapaConversa etapa;
    private DadosConversa dados;
    private int tentativasInvalidas;
    private Instant ultimaInteracao;
    private Instant iniciadaEm;
    private boolean nova;

    private Conversa(Long id, WaId waId, EtapaConversa etapa, DadosConversa dados, int tentativasInvalidas,
                     Instant ultimaInteracao, Instant iniciadaEm, boolean nova) {
        this.id = id;
        this.waId = Objects.requireNonNull(waId, "waId");
        this.etapa = Objects.requireNonNull(etapa, "etapa");
        this.dados = Objects.requireNonNull(dados, "dados");
        this.tentativasInvalidas = tentativasInvalidas;
        this.ultimaInteracao = Objects.requireNonNull(ultimaInteracao, "ultimaInteracao");
        this.iniciadaEm = Objects.requireNonNull(iniciadaEm, "iniciadaEm");
        this.nova = nova;
    }

    /** Primeira mensagem de um número: a próxima resposta registrada envia as boas-vindas. */
    public static Conversa nova(WaId waId, Instant agora) {
        return new Conversa(null, waId, EtapaConversa.AGUARDANDO_RG, DadosConversa.vazio(), 0, agora, agora, true);
    }

    public static Conversa restaurar(Long id, WaId waId, EtapaConversa etapa, DadosConversa dados,
                                     int tentativasInvalidas, Instant ultimaInteracao, Instant iniciadaEm) {
        return new Conversa(Objects.requireNonNull(id, "id"), waId, etapa, dados, tentativasInvalidas,
                ultimaInteracao, iniciadaEm, false);
    }

    // ---------------------------------------------------------------- entrada do paciente

    /**
     * Registra uma resposta do paciente e devolve a próxima ação.
     *
     * @param texto texto da mensagem (ou transcrição do áudio)
     * @param agora instante atual
     * @param hoje  data atual no fuso do consultório (para calcular idade)
     */
    public AcaoConversa registrarResposta(String texto, Instant agora, LocalDate hoje, RegrasConversa regras) {
        String resposta = texto == null ? "" : texto.trim();
        if (nova || expirou(agora, regras) || etapa == EtapaConversa.CONCLUIDA) {
            reiniciar(agora);
            return AcaoConversa.responder(RespostaAgente.de(TipoResposta.BOAS_VINDAS));
        }
        if (etapa.aguardaEquipe()) {
            return AcaoConversa.silencio();
        }
        ultimaInteracao = agora;
        TratadorEtapa tratador = FluxoConversa.PADRAO.tratadorDe(etapa);
        ContextoEtapa contexto = new Contexto(hoje, regras);
        try {
            return tratador.tratar(resposta, contexto);
        } catch (DadoInvalidoException e) {
            return respostaInvalida(regras);
        }
    }

    /** O paciente mandou algo que não é texto (figurinha, imagem) ou um áudio sem transcrição. */
    public AcaoConversa conteudoNaoTextual(Instant agora, LocalDate hoje, RegrasConversa regras) {
        if (nova || expirou(agora, regras) || etapa.ehFinal()) {
            return registrarResposta("", agora, hoje, regras);
        }
        ultimaInteracao = agora;
        return AcaoConversa.responder(RespostaAgente.de(TipoResposta.PEDIR_TEXTO));
    }

    public boolean expirou(Instant agora, RegrasConversa regras) {
        return ultimaInteracao.plus(regras.expiracao()).isBefore(agora);
    }

    private void reiniciar(Instant agora) {
        nova = false;
        dados = DadosConversa.vazio();
        iniciadaEm = agora;
        ultimaInteracao = agora;
        mudarPara(EtapaConversa.AGUARDANDO_RG);
    }

    // ---------------------------------------------------------------- resultados dos comandos

    /** Resultado de {@link IdentificarPaciente}: paciente já cadastrado. */
    public AcaoConversa pacienteEncontrado(long pacienteId, String primeiroNome, Optional<LocalDateTime> consultaFutura) {
        exigirEtapa(EtapaConversa.AGUARDANDO_RG);
        dados = dados.comPaciente(pacienteId, primeiroNome, Optional.empty());
        if (consultaFutura.isPresent()) {
            dados = dados.comConsultaExistente(consultaFutura.get());
            mudarPara(EtapaConversa.AGUARDANDO_CONFIRMACAO_NOVA_CONSULTA);
            return AcaoConversa.responder(RespostaAgente.de(TipoResposta.CONSULTA_EXISTENTE)
                    .comNome(dados.nomePaciente()).comDataHora(consultaFutura.get()));
        }
        mudarPara(EtapaConversa.AGUARDANDO_PROBLEMA);
        return AcaoConversa.responder(RespostaAgente.de(TipoResposta.PACIENTE_ENCONTRADO).comNome(dados.nomePaciente()));
    }

    /** Resultado de {@link IdentificarPaciente}: primeiro contato, inicia o cadastro. */
    public AcaoConversa pacienteNaoEncontrado() {
        exigirEtapa(EtapaConversa.AGUARDANDO_RG);
        mudarPara(EtapaConversa.AGUARDANDO_NOME);
        return AcaoConversa.responder(RespostaAgente.de(TipoResposta.PEDIR_NOME));
    }

    /** Resultado de {@link ComandoConversa.CadastrarPaciente}: paciente e prontuário criados. */
    public AcaoConversa pacienteCadastrado(long pacienteId, String primeiroNome, String numeroProntuario) {
        exigirEtapa(EtapaConversa.AGUARDANDO_CONSENTIMENTO);
        dados = dados.comPaciente(pacienteId, primeiroNome, Optional.of(numeroProntuario));
        mudarPara(EtapaConversa.AGUARDANDO_PROBLEMA);
        return AcaoConversa.responder(RespostaAgente.de(TipoResposta.CADASTRO_CONCLUIDO)
                .comNome(dados.nomePaciente()).comProntuario(numeroProntuario));
    }

    /** Resultado de {@link RegistrarQueixa}. Emergências avisam o consultório e já oferecem o primeiro horário. */
    public AcaoConversa queixaRegistrada(long queixaId, String nomeProblema, Prioridade prioridade) {
        exigirEtapa(EtapaConversa.AGUARDANDO_PROBLEMA);
        dados = dados.comQueixa(queixaId, nomeProblema, prioridade);
        mudarPara(EtapaConversa.AGUARDANDO_DATA_HORA);
        if (prioridade == Prioridade.EMERGENCIA) {
            return AcaoConversa.responder(RespostaAgente.de(TipoResposta.EMERGENCIA))
                    .comAlerta(alerta(TipoAlerta.EMERGENCIA, nomeProblema))
                    .comComando(new BuscarPrimeiroHorarioLivre());
        }
        return AcaoConversa.responder(RespostaAgente.de(TipoResposta.PEDIR_DATA_HORA));
    }

    /** Resultado de {@link InterpretarDataHora} quando nem o parser nem a IA entenderam o texto. */
    public AcaoConversa dataHoraNaoEntendida(RegrasConversa regras) {
        exigirEtapaDeAgendamento();
        return respostaInvalida(regras);
    }

    /** Resultado da consulta à agenda (pedido do paciente, oferta de emergência ou conflito de concorrência). */
    public AcaoConversa disponibilidadeVerificada(Disponibilidade disponibilidade, RegrasConversa regras) {
        exigirEtapaDeAgendamento();
        return switch (disponibilidade) {
            case Disponibilidade.HorarioDisponivel(Horario horario) -> AcaoConversa.executar(reservar(horario));
            case Disponibilidade.HorarioSugerido(Horario horario, boolean pedidoIndisponivel) ->
                    sugerir(horario, pedidoIndisponivel, regras);
            case Disponibilidade.SemHorarioLivre() -> encaminharParaEquipe(TipoResposta.SEM_HORARIO_LIVRE,
                    "Não há horário livre no período de busca da agenda.");
        };
    }

    /** Resultado de {@link ReservarHorario}: consulta gravada na agenda. */
    public AcaoConversa consultaAgendada(Horario horario) {
        exigirEtapaDeAgendamento();
        dados = dados.comHorarioSugerido(Optional.empty());
        mudarPara(EtapaConversa.CONCLUIDA);
        return AcaoConversa.responder(RespostaAgente.de(TipoResposta.AGENDADO)
                .comNome(dados.nomePaciente()).comDataHora(horario.inicio()));
    }

    // ---------------------------------------------------------------- regras internas

    private AcaoConversa sugerir(Horario horario, boolean pedidoIndisponivel, RegrasConversa regras) {
        if (!regras.confirmarSugestao()) {
            return AcaoConversa.executar(reservar(horario));
        }
        dados = dados.comHorarioSugerido(Optional.of(horario));
        mudarPara(EtapaConversa.AGUARDANDO_CONFIRMACAO_SUGESTAO);
        TipoResposta tipo = pedidoIndisponivel ? TipoResposta.SUGERIR_HORARIO : TipoResposta.OFERECER_HORARIO;
        return AcaoConversa.responder(RespostaAgente.de(tipo).comNome(dados.nomePaciente()).comDataHora(horario.inicio()));
    }

    private ReservarHorario reservar(Horario horario) {
        return new ReservarHorario(ContextoEtapa.exigir(dados.pacienteId(), "paciente"), dados.queixaId(), horario);
    }

    private AcaoConversa respostaInvalida(RegrasConversa regras) {
        tentativasInvalidas++;
        if (tentativasInvalidas >= regras.maxTentativasPorEtapa()) {
            return encaminharParaEquipe(TipoResposta.ATENDIMENTO_HUMANO,
                    "O paciente não conseguiu responder à etapa " + descricaoEtapa(etapa) + " após "
                            + tentativasInvalidas + " tentativas.");
        }
        return AcaoConversa.responder(RespostaAgente.de(TipoResposta.NAO_ENTENDI).comEtapa(etapa));
    }

    private AcaoConversa encaminharParaEquipe(TipoResposta resposta, String resumo) {
        mudarPara(EtapaConversa.ENCAMINHADA_ATENDIMENTO_HUMANO);
        return AcaoConversa.responder(RespostaAgente.de(resposta).comNome(dados.nomePaciente()))
                .comAlerta(alerta(TipoAlerta.ATENDIMENTO_HUMANO, resumo));
    }

    private AlertaConversa alerta(TipoAlerta tipo, String resumo) {
        Prioridade prioridade = dados.prioridade().map(tipo.prioridade()::maisAltaEntre).orElse(tipo.prioridade());
        return new AlertaConversa(tipo, identificacaoParaAlerta(), resumo, prioridade);
    }

    /** Nome do paciente, se já identificado; senão, o final do número de WhatsApp. */
    public String identificacaoParaAlerta() {
        return dados.nomePaciente().or(dados::nome).orElse("WhatsApp final " + waId.finalMascarado());
    }

    private static String descricaoEtapa(EtapaConversa etapa) {
        return etapa.name().toLowerCase().replace('_', ' ');
    }

    private void mudarPara(EtapaConversa novaEtapa) {
        etapa = novaEtapa;
        tentativasInvalidas = 0;
    }

    private void exigirEtapa(EtapaConversa esperada) {
        if (etapa != esperada) {
            throw new TransicaoInvalidaException("Conversa na etapa " + etapa + ", esperado " + esperada + ".");
        }
    }

    private void exigirEtapaDeAgendamento() {
        if (etapa != EtapaConversa.AGUARDANDO_DATA_HORA && etapa != EtapaConversa.AGUARDANDO_CONFIRMACAO_SUGESTAO) {
            throw new TransicaoInvalidaException("Conversa na etapa " + etapa + " não está agendando.");
        }
    }

    /** Visão restrita desta conversa entregue ao {@link TratadorEtapa} da etapa atual. */
    private final class Contexto implements ContextoEtapa {

        private final LocalDate hoje;
        private final RegrasConversa regras;

        private Contexto(LocalDate hoje, RegrasConversa regras) {
            this.hoje = hoje;
            this.regras = regras;
        }

        @Override
        public DadosConversa dados() {
            return dados;
        }

        @Override
        public void atualizar(DadosConversa novosDados) {
            dados = Objects.requireNonNull(novosDados, "dados");
        }

        @Override
        public void avancarPara(EtapaConversa novaEtapa) {
            mudarPara(novaEtapa);
        }

        @Override
        public LocalDate hoje() {
            return hoje;
        }

        @Override
        public AcaoConversa respostaInvalida() {
            return Conversa.this.respostaInvalida(regras);
        }

        @Override
        public AlertaConversa alerta(TipoAlerta tipo, String resumo) {
            return Conversa.this.alerta(tipo, resumo);
        }
    }

    // ---------------------------------------------------------------- consultas

    public Optional<Long> id() {
        return Optional.ofNullable(id);
    }

    public WaId waId() {
        return waId;
    }

    public EtapaConversa etapa() {
        return etapa;
    }

    public DadosConversa dados() {
        return dados;
    }

    public int tentativasInvalidas() {
        return tentativasInvalidas;
    }

    public Instant ultimaInteracao() {
        return ultimaInteracao;
    }

    public Instant iniciadaEm() {
        return iniciadaEm;
    }

    /** Janela de 24h da Meta, contada a partir da última mensagem do paciente. */
    public JanelaAtendimento janelaAtendimento() {
        return new JanelaAtendimento(ultimaInteracao);
    }
}
