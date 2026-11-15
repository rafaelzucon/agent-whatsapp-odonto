package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.porta.entrada.CadastrarPacienteUseCase.CadastrarPacienteCommand;
import br.com.consultorio.odonto.aplicacao.porta.entrada.RegistrarQueixaUseCase.QueixaRegistrada;
import br.com.consultorio.odonto.aplicacao.porta.saida.ClassificacaoIA;
import br.com.consultorio.odonto.dominio.modelo.DocumentoRg;
import br.com.consultorio.odonto.dominio.modelo.OrigemClassificacao;
import br.com.consultorio.odonto.dominio.modelo.Prioridade;
import br.com.consultorio.odonto.dominio.modelo.WaId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrarQueixaServiceTest {

    private final Cenario cenario = new Cenario();
    private long pacienteId;

    @BeforeEach
    void cadastrar() {
        pacienteId = cenario.cadastrar.cadastrar(new CadastrarPacienteCommand(DocumentoRg.de("123456789", "SP"),
                "Maria da Silva", LocalDate.of(1990, 5, 10), Optional.empty(), new WaId(Cenario.PACIENTE)))
                .pacienteId();
    }

    @Test
    @DisplayName("aceita a classificação da IA quando a confiança atinge o mínimo")
    void aceitaIA() {
        cenario.classificadorIA.responder("CARIE", 0.9);

        QueixaRegistrada queixa = cenario.registrarQueixa.registrar(pacienteId, "tem uma mancha escura no dente");

        assertThat(queixa.codigoProblema()).isEqualTo("CARIE");
        assertThat(queixa.origem()).isEqualTo(OrigemClassificacao.IA);
        assertThat(queixa.confianca()).isEqualTo(0.9);
        assertThat(cenario.prontuarios.buscarPorPacienteId(pacienteId).orElseThrow().queixas()).hasSize(1);
    }

    @Test
    @DisplayName("confiança baixa da IA cai para palavras-chave")
    void confiancaBaixaUsaPalavraChave() {
        cenario.classificadorIA.responder("CARIE", 0.3);

        QueixaRegistrada queixa = cenario.registrarQueixa.registrar(pacienteId, "quero fazer uma limpeza");

        assertThat(queixa.codigoProblema()).isEqualTo("LIMPEZA");
        assertThat(queixa.origem()).isEqualTo(OrigemClassificacao.PALAVRA_CHAVE);
    }

    @Test
    @DisplayName("IA fora do ar cai para palavras-chave")
    void falhaDaIAUsaPalavraChave() {
        QueixaRegistrada queixa = cenario.registrarQueixa.registrar(pacienteId, "acho que tenho uma carie");

        assertThat(cenario.classificadorIA.chamadas).isEqualTo(1);
        assertThat(queixa.codigoProblema()).isEqualTo("CARIE");
        assertThat(queixa.origem()).isEqualTo(OrigemClassificacao.PALAVRA_CHAVE);
    }

    @Test
    @DisplayName("código inexistente no catálogo é descartado")
    void codigoInexistenteDescartado() {
        cenario.classificadorIA.responder("INVENTADO", 0.99);

        QueixaRegistrada queixa = cenario.registrarQueixa.registrar(pacienteId, "quero fazer uma limpeza");

        assertThat(queixa.codigoProblema()).isEqualTo("LIMPEZA");
    }

    @Test
    @DisplayName("sem IA e sem palavra-chave, registra avaliação geral")
    void semClassificacaoViraAvaliacaoGeral() {
        QueixaRegistrada queixa = cenario.registrarQueixa.registrar(pacienteId, "não sei explicar direito");

        assertThat(queixa.codigoProblema()).isEqualTo("AVALIACAO_GERAL");
        assertThat(queixa.confianca()).isZero();
        assertThat(queixa.prioridade()).isEqualTo(Prioridade.ROTINA);
    }

    @Test
    @DisplayName("avaliação geral da IA cede lugar à palavra-chave")
    void avaliacaoGeralDaIACedeParaPalavraChave() {
        cenario.classificadorIA.responder("AVALIACAO_GERAL", 0.8);

        assertThat(cenario.registrarQueixa.registrar(pacienteId, "quero tirar o tartaro").codigoProblema())
                .isEqualTo("LIMPEZA");
        assertThat(cenario.registrarQueixa.registrar(pacienteId, "não sei explicar").origem())
                .isEqualTo(OrigemClassificacao.IA);
    }

    @Test
    @DisplayName("sinais de alarme elevam a prioridade para emergência")
    void sinaisDeAlarmeElevamPrioridade() {
        cenario.classificadorIA.responder("DOR_DE_DENTE", 0.9);

        QueixaRegistrada queixa = cenario.registrarQueixa.registrar(pacienteId,
                "dor de dente e estou com o rosto inchado e febre alta");

        assertThat(queixa.prioridade()).isEqualTo(Prioridade.EMERGENCIA);
    }

    @Test
    @DisplayName("prioridade indicada pela IA só pode subir a do catálogo")
    void prioridadeDaIASoSobe() {
        cenario.classificadorIA.resposta = (q, c) -> new ClassificacaoIA("CARIE", 0.9, List.of(), false,
                Optional.of(Prioridade.URGENCIA));

        assertThat(cenario.registrarQueixa.registrar(pacienteId, "dente com buraco").prioridade())
                .isEqualTo(Prioridade.URGENCIA);
    }
}
