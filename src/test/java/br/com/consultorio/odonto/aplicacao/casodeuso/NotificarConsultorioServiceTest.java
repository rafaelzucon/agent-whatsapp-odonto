package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.dominio.modelo.AlertaConversa;
import br.com.consultorio.odonto.dominio.modelo.Prioridade;
import br.com.consultorio.odonto.dominio.modelo.TipoAlerta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificarConsultorioServiceTest {

    private final Cenario cenario = new Cenario();

    @Test
    @DisplayName("falha em um canal não impede os demais")
    void falhaEmUmCanalNaoBloqueiaOsOutros() {
        cenario.canalWhatsApp.falhar = true;
        cenario.cadastrarPacienteNovo();
        cenario.classificadorIA.responder("DOR_DE_DENTE", 0.9);
        cenario.paciente("dor de dente");
        cenario.paciente("18/11/2026 10:00");

        assertThat(cenario.canalEmail.avisos).singleElement().satisfies(aviso -> {
            assertThat(aviso.nomePaciente()).isEqualTo("Maria da Silva");
            assertThat(aviso.problema()).isEqualTo("Dor de dente");
            assertThat(aviso.numeroProntuario()).isEqualTo("PR-000001");
        });
    }

    @Test
    @DisplayName("canal desabilitado não é acionado")
    void canalDesabilitadoIgnorado() {
        cenario.canalEmail.habilitado = false;

        cenario.notificar.alertar(new AlertaConversa(TipoAlerta.ATENDIMENTO_HUMANO, "Maria", "precisa de ajuda",
                Prioridade.URGENCIA));

        assertThat(cenario.canalEmail.alertas).isEmpty();
        assertThat(cenario.canalWhatsApp.alertas).hasSize(1);
    }
}
