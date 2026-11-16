package br.com.consultorio.odonto.adaptador.saida.persistencia;

import br.com.consultorio.odonto.dominio.modelo.DadosConversa;
import br.com.consultorio.odonto.dominio.modelo.DocumentoRg;
import br.com.consultorio.odonto.dominio.modelo.Horario;
import br.com.consultorio.odonto.dominio.modelo.Prioridade;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DadosConversaJsonTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void idaEVoltaPreservaTodosOsDados() {
        DadosConversa dados = DadosConversa.vazio()
                .comRg(DocumentoRg.de("12345678X", "MG"))
                .comNome("Ana Souza")
                .comDataNascimento(LocalDate.of(2012, 3, 4))
                .comResponsavel("Carlos Souza")
                .comConsultaExistente(LocalDateTime.of(2026, 12, 1, 9, 0))
                .comQueixa(7L, "Pulpite", Prioridade.URGENCIA)
                .comHorarioSugerido(Optional.of(Horario.de(LocalDateTime.of(2026, 11, 20, 14, 30),
                        Duration.ofMinutes(30))));

        String texto = json.writeValueAsString(DadosConversaJson.de(dados));
        DadosConversa restaurado = json.readValue(texto, DadosConversaJson.class).paraDominio();

        assertThat(restaurado).isEqualTo(dados);
    }

    @Test
    void dadosVaziosViramJsonComNulos() {
        String texto = json.writeValueAsString(DadosConversaJson.de(DadosConversa.vazio()));

        assertThat(json.readValue(texto, DadosConversaJson.class).paraDominio()).isEqualTo(DadosConversa.vazio());
    }
}
