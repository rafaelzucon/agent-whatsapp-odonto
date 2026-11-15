package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.porta.entrada.CadastrarPacienteUseCase.CadastrarPacienteCommand;
import br.com.consultorio.odonto.aplicacao.porta.entrada.MensagemRecebidaCommand;
import br.com.consultorio.odonto.dominio.modelo.Consulta;
import br.com.consultorio.odonto.dominio.modelo.DirecaoMensagem;
import br.com.consultorio.odonto.dominio.modelo.DocumentoRg;
import br.com.consultorio.odonto.dominio.modelo.EtapaConversa;
import br.com.consultorio.odonto.dominio.modelo.Horario;
import br.com.consultorio.odonto.dominio.modelo.Mensagem;
import br.com.consultorio.odonto.dominio.modelo.PreferenciaHorario;
import br.com.consultorio.odonto.dominio.modelo.Prioridade;
import br.com.consultorio.odonto.dominio.modelo.Prontuario;
import br.com.consultorio.odonto.dominio.modelo.TipoAlerta;
import br.com.consultorio.odonto.dominio.modelo.WaId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ConduzirConversaServiceTest {

    private static final LocalDateTime QUARTA_10H = LocalDateTime.of(2026, 11, 18, 10, 0);

    private final Cenario cenario = new Cenario();

    private static List<String> tipos(List<String> respostas) {
        return respostas.stream().map(r -> r.split(" \\| ")[0].replace('-', '_')).toList();
    }

    private long cadastrarOutroPaciente(String rg, String telefone) {
        return cenario.cadastrar.cadastrar(new CadastrarPacienteCommand(DocumentoRg.de(rg, "SP"), "João Souza",
                LocalDate.of(1980, 1, 1), Optional.empty(), new WaId(telefone))).pacienteId();
    }

    @Test
    @DisplayName("paciente novo: cadastro, prontuário, queixa classificada pela IA, agendamento e aviso ao consultório")
    void fluxoCompletoDePacienteNovo() {
        assertThat(tipos(cenario.paciente("Olá, quero marcar"))).containsExactly("BOAS_VINDAS");
        assertThat(tipos(cenario.paciente("12.345.678-9 SP"))).containsExactly("PEDIR_NOME");
        assertThat(tipos(cenario.paciente("maria da silva"))).containsExactly("PEDIR_DATA_NASCIMENTO");
        assertThat(tipos(cenario.paciente("10/05/1990"))).containsExactly("PEDIR_CONSENTIMENTO");

        List<String> cadastro = cenario.paciente("sim");
        assertThat(cadastro).singleElement().asString()
                .startsWith("CADASTRO-CONCLUIDO | Maria").contains("PR-000001");

        cenario.classificadorIA.responder("DOR_DE_DENTE", 0.92);
        assertThat(tipos(cenario.paciente("estou com o dente latejando desde ontem"))).containsExactly("PEDIR_DATA_HORA");

        List<String> agendado = cenario.paciente("18/11/2026 10:00");
        assertThat(agendado).singleElement().asString()
                .startsWith("AGENDADO | Maria | quarta-feira, 18/11/2026 às 10:00");
        assertThat(cenario.etapa()).isEqualTo(EtapaConversa.CONCLUIDA);

        Consulta consulta = cenario.consultas.porId.values().iterator().next();
        assertThat(consulta.horario().inicio()).isEqualTo(QUARTA_10H);
        Prontuario prontuario = cenario.prontuarios.buscarPorPacienteId(consulta.pacienteId()).orElseThrow();
        assertThat(prontuario.queixas()).singleElement().satisfies(q -> {
            assertThat(q.problema().codigo()).isEqualTo("DOR_DE_DENTE");
            assertThat(q.consultaId()).isEqualTo(consulta.id());
            assertThat(q.textoOriginal()).isEqualTo("estou com o dente latejando desde ontem");
        });
        assertThat(cenario.pacientes.buscarPorId(consulta.pacienteId()).orElseThrow().telefones())
                .containsExactly(new WaId(Cenario.PACIENTE));

        assertThat(cenario.canalWhatsApp.avisos).singleElement().satisfies(aviso -> {
            assertThat(aviso.nomePaciente()).isEqualTo("Maria da Silva");
            assertThat(aviso.problema()).isEqualTo("Dor de dente");
            assertThat(aviso.parametrosTemplate()).containsExactly("Maria da Silva", "18/11/2026 às 10:00",
                    "Dor de dente", "Urgência");
        });
        assertThat(cenario.canalEmail.avisos).hasSize(1);
        assertThat(cenario.mensagens.porId.values()).filteredOn(m -> m.direcao() == DirecaoMensagem.ENVIADA)
                .hasSize(7).allMatch(Mensagem::automatica);
    }

    @Test
    @DisplayName("paciente existente com horário ocupado recebe o horário livre mais próximo e confirma")
    void horarioOcupadoSugereProximo() {
        long outro = cadastrarOutroPaciente("99999999", "5511777770000");
        cenario.agendar.agendar(new br.com.consultorio.odonto.aplicacao.porta.entrada.AgendarConsultaUseCase
                .AgendarConsultaCommand(outro, Optional.empty(), QUARTA_10H, false));
        long pacienteId = cadastrarOutroPaciente("123456789", "5511666660000");

        cenario.paciente("oi");
        assertThat(cenario.paciente("123456789 SP")).singleElement().asString().startsWith("PACIENTE-ENCONTRADO | João");
        assertThat(cenario.pacientes.buscarPorId(pacienteId).orElseThrow().telefones())
                .contains(new WaId(Cenario.PACIENTE));

        // IA fora do ar: a queixa é classificada por palavra-chave.
        cenario.paciente("acho que tenho uma carie");
        assertThat(cenario.prontuarios.buscarPorPacienteId(pacienteId).orElseThrow().ultimaQueixa().orElseThrow()
                .problema().codigo()).isEqualTo("CARIE");

        assertThat(cenario.paciente("18/11/2026 10:00")).singleElement().asString()
                .startsWith("SUGERIR-HORARIO | João | quarta-feira, 18/11/2026 às 10:30");
        assertThat(cenario.paciente("sim")).singleElement().asString().contains("18/11/2026 às 10:30");
        assertThat(cenario.consultas.listarPorPaciente(pacienteId)).singleElement()
                .extracting(c -> c.horario().inicio()).isEqualTo(QUARTA_10H.plusMinutes(30));
    }

    @Test
    @DisplayName("se outro paciente reservar o horário no mesmo instante, a conversa oferece o próximo livre")
    void corridaPeloMesmoHorario() {
        long outro = cadastrarOutroPaciente("99999999", "5511777770000");
        cenario.cadastrarPacienteNovo();
        cenario.classificadorIA.responder("LIMPEZA", 0.9);
        cenario.paciente("quero fazer uma limpeza");
        cenario.consultas.concorrente = Optional.of(Consulta.agendar(outro, Optional.empty(),
                Horario.de(QUARTA_10H, Duration.ofMinutes(30)), Fakes.AGORA));

        assertThat(cenario.paciente("18/11/2026 10:00")).singleElement().asString()
                .startsWith("SUGERIR-HORARIO").contains("10:30");
        assertThat(cenario.etapa()).isEqualTo(EtapaConversa.AGUARDANDO_CONFIRMACAO_SUGESTAO);
    }

    @Test
    @DisplayName("emergência detectada avisa o consultório e oferece o primeiro horário livre")
    void emergencia() {
        cenario.cadastrarPacienteNovo();
        cenario.classificadorIA.responder("DOR_DE_DENTE", 0.8);

        List<String> respostas = cenario.paciente("estou com o rosto inchado e febre alta");

        assertThat(tipos(respostas)).containsExactly("EMERGENCIA", "OFERECER_HORARIO");
        // Segunda 09:00 + 2h de antecedência mínima = 11:00.
        assertThat(respostas.get(1)).contains("16/11/2026 às 11:00");
        assertThat(cenario.canalWhatsApp.alertas).singleElement().satisfies(alerta -> {
            assertThat(alerta.tipo()).isEqualTo(TipoAlerta.EMERGENCIA);
            assertThat(alerta.prioridade()).isEqualTo(Prioridade.EMERGENCIA);
            assertThat(alerta.identificacao()).isEqualTo("Maria");
        });
        assertThat(cenario.paciente("sim")).singleElement().asString().startsWith("AGENDADO");
        assertThat(cenario.canalWhatsApp.avisos).singleElement()
                .extracting(a -> a.prioridade()).isEqualTo(Prioridade.EMERGENCIA);
    }

    @Test
    @DisplayName("data em linguagem natural vai para a IA; se ela falhar, pede de novo com a dica")
    void dataPelaIA() {
        cenario.cadastrarPacienteNovo();
        cenario.paciente("dor de dente");

        cenario.interpretadorIA.falhar = true;
        assertThat(cenario.paciente("quando der pra vocês")).singleElement().asString()
                .startsWith("NAO-ENTENDI").contains("dica AGUARDANDO_DATA_HORA");

        cenario.interpretadorIA.falhar = false;
        cenario.interpretadorIA.resposta = Optional.of(
                PreferenciaHorario.de(LocalDate.of(2026, 11, 18), LocalTime.of(14, 0), null));
        assertThat(cenario.paciente("quarta lá pelas duas da tarde")).singleElement().asString()
                .startsWith("AGENDADO").contains("18/11/2026 às 14:00");
        assertThat(cenario.interpretadorIA.chamadas).isEqualTo(2);
    }

    @Test
    @DisplayName("data clara é resolvida pelo interpretador determinístico, sem chamar a IA")
    void dataSemIA() {
        cenario.cadastrarPacienteNovo();
        cenario.paciente("dor de dente");

        cenario.paciente("18/11/2026 10:00");

        assertThat(cenario.interpretadorIA.chamadas).isZero();
    }

    @Test
    @DisplayName("áudio sem transcritor pede texto; com transcritor, a transcrição segue a conversa")
    void audio() {
        cenario.paciente("Olá");
        assertThat(tipos(cenario.processar(MensagemRecebidaCommand.audio(cenario.proximoWamid(), Cenario.PACIENTE,
                "midia-1", cenario.relogio.agora())))).containsExactly("PEDIR_TEXTO");

        cenario.transcritor.disponivel = true;
        cenario.transcritor.transcricao = Optional.of("12.345.678-9 SP");
        assertThat(tipos(cenario.processar(MensagemRecebidaCommand.audio(cenario.proximoWamid(), Cenario.PACIENTE,
                "midia-2", cenario.relogio.agora())))).containsExactly("PEDIR_NOME");
        assertThat(cenario.mensagens.porId.values()).anyMatch(m -> m.midiaId().equals(Optional.of("midia-2"))
                && m.texto().equals(Optional.of("12.345.678-9 SP")));
    }

    @Test
    @DisplayName("falha no envio pela Meta não impede que o estado da conversa seja gravado")
    void falhaNoEnvio() {
        cenario.enviador.falhar = true;

        cenario.paciente("Olá");

        assertThat(cenario.etapa()).isEqualTo(EtapaConversa.AGUARDANDO_RG);
        assertThat(cenario.mensagens.porId.values()).filteredOn(m -> m.direcao() == DirecaoMensagem.ENVIADA)
                .singleElement().satisfies(m -> assertThat(m.waMessageId()).isEmpty());
    }

    @Test
    @DisplayName("três respostas inválidas encaminham para a equipe; depois o agente fica em silêncio")
    void encaminhamentoParaEquipe() {
        cenario.paciente("Olá");
        cenario.paciente("abc");
        cenario.paciente("não sei");

        assertThat(tipos(cenario.paciente("???"))).containsExactly("ATENDIMENTO_HUMANO");
        assertThat(cenario.canalEmail.alertas).singleElement()
                .extracting(a -> a.tipo()).isEqualTo(TipoAlerta.ATENDIMENTO_HUMANO);
        assertThat(cenario.paciente("alô?")).isEmpty();

        cenario.relogio.avancar(Duration.ofHours(3));
        assertThat(tipos(cenario.paciente("oi de novo"))).containsExactly("BOAS_VINDAS");
    }

    @Test
    @DisplayName("paciente com consulta futura é avisado antes de marcar outra")
    void consultaExistente() {
        long pacienteId = cadastrarOutroPaciente("123456789", Cenario.PACIENTE);
        cenario.agendar.agendar(new br.com.consultorio.odonto.aplicacao.porta.entrada.AgendarConsultaUseCase
                .AgendarConsultaCommand(pacienteId, Optional.empty(), QUARTA_10H, false));

        cenario.paciente("oi");
        assertThat(cenario.paciente("123456789 SP")).singleElement().asString()
                .startsWith("CONSULTA-EXISTENTE | João | quarta-feira, 18/11/2026 às 10:00");
        assertThat(tipos(cenario.paciente("não"))).containsExactly("ENCERRAMENTO");
        assertThat(cenario.etapa()).isEqualTo(EtapaConversa.CONCLUIDA);
    }

    @Test
    @DisplayName("a última interação da conversa usa o instante do relógio")
    void ultimaInteracaoUsaInstanteDoRelogio() {
        Instant antes = cenario.relogio.agora();
        cenario.paciente("Olá");
        assertThat(cenario.conversas.buscarPorWaId(new WaId(Cenario.PACIENTE)).orElseThrow().ultimaInteracao())
                .isEqualTo(antes);
    }
}
