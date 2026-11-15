package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.excecao.TransicaoInvalidaException;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa.BuscarPrimeiroHorarioLivre;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa.CadastrarPaciente;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa.IdentificarPaciente;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa.InterpretarDataHora;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa.RegistrarQueixa;
import br.com.consultorio.odonto.dominio.modelo.ComandoConversa.ReservarHorario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversaTest {

    private static final Instant AGORA = Instant.parse("2026-11-16T12:00:00Z");
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 16);
    private static final RegrasConversa REGRAS = new RegrasConversa(Duration.ofMinutes(120), 3, true);
    private static final Horario HORARIO = Horario.de(LocalDateTime.of(2026, 11, 18, 10, 0), Duration.ofMinutes(30));

    private Conversa conversa;

    @BeforeEach
    void iniciar() {
        conversa = Conversa.nova(new WaId("5511999991234"), AGORA);
    }

    private AcaoConversa responder(String texto) {
        return conversa.registrarResposta(texto, AGORA, HOJE, REGRAS);
    }

    private void ateAguardarProblemaComPacienteExistente() {
        responder("oi");
        responder("12.345.678-9 SP");
        conversa.pacienteEncontrado(7L, "Maria", Optional.empty());
    }

    @Test
    @DisplayName("primeira mensagem envia boas-vindas e pede o RG")
    void boasVindas() {
        AcaoConversa acao = responder("Olá, quero marcar uma consulta");

        assertThat(acao.respostas()).extracting(RespostaAgente::tipo).containsExactly(TipoResposta.BOAS_VINDAS);
        assertThat(conversa.etapa()).isEqualTo(EtapaConversa.AGUARDANDO_RG);
    }

    @Test
    @DisplayName("paciente existente vai direto para a pergunta do problema")
    void pacienteExistente() {
        responder("oi");
        AcaoConversa acao = responder("12.345.678-9 SP");

        assertThat(acao.comando()).contains(new IdentificarPaciente(DocumentoRg.de("123456789", "SP")));

        AcaoConversa encontrado = conversa.pacienteEncontrado(7L, "Maria", Optional.empty());

        assertThat(encontrado.respostas().getFirst().tipo()).isEqualTo(TipoResposta.PACIENTE_ENCONTRADO);
        assertThat(encontrado.respostas().getFirst().nomePaciente()).contains("Maria");
        assertThat(conversa.etapa()).isEqualTo(EtapaConversa.AGUARDANDO_PROBLEMA);
        assertThat(conversa.dados().rg()).isEmpty();
    }

    @Test
    @DisplayName("paciente novo maior de idade passa por nome, nascimento e consentimento")
    void cadastroCompleto() {
        responder("oi");
        responder("123456789 SP");
        assertThat(conversa.pacienteNaoEncontrado().respostas().getFirst().tipo()).isEqualTo(TipoResposta.PEDIR_NOME);

        assertThat(responder("maria da silva").respostas().getFirst().tipo())
                .isEqualTo(TipoResposta.PEDIR_DATA_NASCIMENTO);
        assertThat(responder("10/05/1990").respostas().getFirst().tipo())
                .isEqualTo(TipoResposta.PEDIR_CONSENTIMENTO);

        AcaoConversa consentimento = responder("Sim");
        assertThat(consentimento.comando()).hasValueSatisfying(comando -> {
            CadastrarPaciente cadastro = (CadastrarPaciente) comando;
            assertThat(cadastro.nomeCompleto()).isEqualTo("Maria da Silva");
            assertThat(cadastro.responsavelLegal()).isEmpty();
        });

        AcaoConversa cadastrado = conversa.pacienteCadastrado(1L, "Maria", "PR-000001");
        assertThat(cadastrado.respostas().getFirst().tipo()).isEqualTo(TipoResposta.CADASTRO_CONCLUIDO);
        assertThat(cadastrado.respostas().getFirst().prontuario()).contains("PR-000001");
        assertThat(conversa.etapa()).isEqualTo(EtapaConversa.AGUARDANDO_PROBLEMA);
    }

    @Test
    @DisplayName("paciente menor de idade precisa informar o responsável legal")
    void menorPedeResponsavel() {
        responder("oi");
        responder("123456789 SP");
        conversa.pacienteNaoEncontrado();
        responder("Pedro Souza");

        assertThat(responder("01/01/2015").respostas().getFirst().tipo()).isEqualTo(TipoResposta.PEDIR_RESPONSAVEL);
        assertThat(conversa.etapa()).isEqualTo(EtapaConversa.AGUARDANDO_RESPONSAVEL);

        responder("Ana Souza");
        AcaoConversa acao = responder("concordo");
        assertThat(((CadastrarPaciente) acao.comando().orElseThrow()).responsavelLegal()).contains("Ana Souza");
    }

    @Test
    @DisplayName("consentimento recusado descarta os dados, avisa o consultório e silencia")
    void consentimentoRecusado() {
        responder("oi");
        responder("123456789 SP");
        conversa.pacienteNaoEncontrado();
        responder("Maria Silva");
        responder("10/05/1990");

        AcaoConversa acao = responder("não");

        assertThat(acao.respostas().getFirst().tipo()).isEqualTo(TipoResposta.SEM_CONSENTIMENTO);
        assertThat(acao.alerta()).hasValueSatisfying(alerta -> {
            assertThat(alerta.tipo()).isEqualTo(TipoAlerta.CONSENTIMENTO_RECUSADO);
            assertThat(alerta.identificacao()).isEqualTo("WhatsApp final ***1234");
        });
        assertThat(conversa.etapa()).isEqualTo(EtapaConversa.ENCERRADA_SEM_CONSENTIMENTO);
        assertThat(conversa.dados()).isEqualTo(DadosConversa.vazio());
        assertThat(responder("alô?").respostas()).isEmpty();
    }

    @Test
    @DisplayName("três respostas inválidas encaminham ao atendimento humano")
    void tentativasInvalidas() {
        responder("oi");

        assertThat(responder("não sei").respostas().getFirst().tipo()).isEqualTo(TipoResposta.NAO_ENTENDI);
        assertThat(responder("???").respostas().getFirst().etapa()).contains(EtapaConversa.AGUARDANDO_RG);
        AcaoConversa terceira = responder("abc");

        assertThat(terceira.respostas().getFirst().tipo()).isEqualTo(TipoResposta.ATENDIMENTO_HUMANO);
        assertThat(terceira.alerta()).map(AlertaConversa::tipo).contains(TipoAlerta.ATENDIMENTO_HUMANO);
        assertThat(conversa.etapa()).isEqualTo(EtapaConversa.ENCAMINHADA_ATENDIMENTO_HUMANO);
    }

    @Test
    @DisplayName("conversa parada além do tempo de expiração recomeça do início")
    void expiracaoReinicia() {
        ateAguardarProblemaComPacienteExistente();

        AcaoConversa acao = conversa.registrarResposta("voltei", AGORA.plus(Duration.ofHours(3)), HOJE, REGRAS);

        assertThat(acao.respostas().getFirst().tipo()).isEqualTo(TipoResposta.BOAS_VINDAS);
        assertThat(conversa.etapa()).isEqualTo(EtapaConversa.AGUARDANDO_RG);
        assertThat(conversa.dados().pacienteId()).isEmpty();
    }

    @Test
    @DisplayName("queixa registrada pede data e hora; data disponível vira reserva e confirmação")
    void fluxoDeAgendamento() {
        ateAguardarProblemaComPacienteExistente();

        assertThat(responder("dor de dente").comando()).contains(new RegistrarQueixa(7L, "dor de dente"));
        AcaoConversa queixa = conversa.queixaRegistrada(30L, "Dor de dente", Prioridade.URGENCIA);
        assertThat(queixa.respostas().getFirst().tipo()).isEqualTo(TipoResposta.PEDIR_DATA_HORA);

        assertThat(responder("amanhã 10h").comando()).contains(new InterpretarDataHora("amanhã 10h"));
        AcaoConversa reserva = conversa.disponibilidadeVerificada(new Disponibilidade.HorarioDisponivel(HORARIO), REGRAS);
        assertThat(reserva.comando()).contains(new ReservarHorario(7L, Optional.of(30L), HORARIO));

        AcaoConversa agendada = conversa.consultaAgendada(HORARIO);
        assertThat(agendada.respostas().getFirst().tipo()).isEqualTo(TipoResposta.AGENDADO);
        assertThat(agendada.respostas().getFirst().dataHora()).contains(HORARIO.inicio());
        assertThat(conversa.etapa()).isEqualTo(EtapaConversa.CONCLUIDA);
    }

    @Test
    @DisplayName("horário indisponível gera sugestão; SIM reserva o horário sugerido")
    void sugestaoConfirmada() {
        ateAguardarProblemaComPacienteExistente();
        responder("dor de dente");
        conversa.queixaRegistrada(30L, "Dor de dente", Prioridade.URGENCIA);
        responder("amanhã 10h");

        AcaoConversa sugestao = conversa.disponibilidadeVerificada(
                new Disponibilidade.HorarioSugerido(HORARIO, true), REGRAS);

        assertThat(sugestao.respostas().getFirst().tipo()).isEqualTo(TipoResposta.SUGERIR_HORARIO);
        assertThat(conversa.etapa()).isEqualTo(EtapaConversa.AGUARDANDO_CONFIRMACAO_SUGESTAO);
        assertThat(responder("sim").comando()).contains(new ReservarHorario(7L, Optional.of(30L), HORARIO));
    }

    @Test
    @DisplayName("na confirmação, NÃO pede nova data e outro texto é reinterpretado")
    void sugestaoRecusada() {
        ateAguardarProblemaComPacienteExistente();
        responder("dor de dente");
        conversa.queixaRegistrada(30L, "Dor de dente", Prioridade.URGENCIA);
        responder("amanhã 10h");
        conversa.disponibilidadeVerificada(new Disponibilidade.HorarioSugerido(HORARIO, true), REGRAS);

        assertThat(responder("sexta às 15h").comando()).contains(new InterpretarDataHora("sexta às 15h"));
        assertThat(responder("não").respostas().getFirst().tipo()).isEqualTo(TipoResposta.PEDIR_DATA_HORA);
        assertThat(conversa.etapa()).isEqualTo(EtapaConversa.AGUARDANDO_DATA_HORA);
    }

    @Test
    @DisplayName("sem confirmação configurada, a sugestão é reservada direto")
    void sugestaoSemConfirmacao() {
        RegrasConversa semConfirmacao = new RegrasConversa(Duration.ofMinutes(120), 3, false);
        ateAguardarProblemaComPacienteExistente();
        responder("dor de dente");
        conversa.queixaRegistrada(30L, "Dor de dente", Prioridade.ROTINA);
        responder("amanhã 10h");

        AcaoConversa acao = conversa.disponibilidadeVerificada(
                new Disponibilidade.HorarioSugerido(HORARIO, true), semConfirmacao);

        assertThat(acao.comando()).contains(new ReservarHorario(7L, Optional.of(30L), HORARIO));
    }

    @Test
    @DisplayName("emergência avisa o paciente, alerta o consultório e busca o primeiro horário")
    void emergencia() {
        ateAguardarProblemaComPacienteExistente();
        responder("meu rosto está inchado");

        AcaoConversa acao = conversa.queixaRegistrada(31L, "Abscesso dentário", Prioridade.EMERGENCIA);

        assertThat(acao.respostas().getFirst().tipo()).isEqualTo(TipoResposta.EMERGENCIA);
        assertThat(acao.alerta()).hasValueSatisfying(alerta -> {
            assertThat(alerta.tipo()).isEqualTo(TipoAlerta.EMERGENCIA);
            assertThat(alerta.prioridade()).isEqualTo(Prioridade.EMERGENCIA);
            assertThat(alerta.identificacao()).isEqualTo("Maria");
        });
        assertThat(acao.comando()).contains(new BuscarPrimeiroHorarioLivre());

        AcaoConversa oferta = conversa.disponibilidadeVerificada(
                new Disponibilidade.HorarioSugerido(HORARIO, false), REGRAS);
        assertThat(oferta.respostas().getFirst().tipo()).isEqualTo(TipoResposta.OFERECER_HORARIO);
    }

    @Test
    @DisplayName("paciente com consulta futura escolhe se quer agendar outra")
    void consultaExistente() {
        responder("oi");
        responder("123456789 SP");
        LocalDateTime futura = LocalDateTime.of(2026, 11, 20, 9, 0);

        AcaoConversa acao = conversa.pacienteEncontrado(7L, "Maria", Optional.of(futura));

        assertThat(acao.respostas().getFirst().tipo()).isEqualTo(TipoResposta.CONSULTA_EXISTENTE);
        assertThat(acao.respostas().getFirst().dataHora()).contains(futura);
        assertThat(conversa.etapa()).isEqualTo(EtapaConversa.AGUARDANDO_CONFIRMACAO_NOVA_CONSULTA);

        assertThat(responder("não").respostas().getFirst().tipo()).isEqualTo(TipoResposta.ENCERRAMENTO);
        assertThat(conversa.etapa()).isEqualTo(EtapaConversa.CONCLUIDA);
    }

    @Test
    @DisplayName("sem horário livre encaminha para a equipe")
    void semHorarioLivre() {
        ateAguardarProblemaComPacienteExistente();
        responder("limpeza");
        conversa.queixaRegistrada(32L, "Limpeza", Prioridade.ROTINA);
        responder("amanhã");

        AcaoConversa acao = conversa.disponibilidadeVerificada(new Disponibilidade.SemHorarioLivre(), REGRAS);

        assertThat(acao.respostas().getFirst().tipo()).isEqualTo(TipoResposta.SEM_HORARIO_LIVRE);
        assertThat(conversa.etapa()).isEqualTo(EtapaConversa.ENCAMINHADA_ATENDIMENTO_HUMANO);
    }

    @Test
    @DisplayName("resultado de comando fora da etapa esperada é transição inválida")
    void transicaoInvalida() {
        responder("oi");

        assertThatThrownBy(() -> conversa.consultaAgendada(HORARIO)).isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    @DisplayName("conteúdo não textual pede resposta por texto")
    void conteudoNaoTextual() {
        responder("oi");

        AcaoConversa acao = conversa.conteudoNaoTextual(AGORA, HOJE, REGRAS);

        assertThat(acao.respostas().getFirst().tipo()).isEqualTo(TipoResposta.PEDIR_TEXTO);
        assertThat(conversa.etapa()).isEqualTo(EtapaConversa.AGUARDANDO_RG);
    }
}
