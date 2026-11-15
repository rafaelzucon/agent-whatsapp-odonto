package br.com.consultorio.odonto.dominio.servico;

import br.com.consultorio.odonto.dominio.modelo.Prioridade;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DetectorSinaisAlarmeTest {

    private final DetectorSinaisAlarme detector = new DetectorSinaisAlarme();

    @Test
    void detectaSinaisIgnorandoAcentosEMaiusculas() {
        assertThat(detector.sinaisEncontrados("Estou com o ROSTO INCHADO e FEBRE ALTA desde ontem"))
                .containsExactlyInAnyOrder("rosto inchado", "febre alta");
        assertThat(detector.possuiSinaisAlarme("tenho dificuldade para engolir")).isTrue();
        assertThat(detector.possuiSinaisAlarme("o sangramento que não para depois da extração")).isTrue();
        assertThat(detector.prioridadePara("Bati o dente na escada")).isEqualTo(Prioridade.EMERGENCIA);
    }

    @Test
    void queixaComumNaoAcionaAlarme() {
        assertThat(detector.possuiSinaisAlarme("dor de dente quando tomo gelado")).isFalse();
        assertThat(detector.prioridadePara("quero fazer uma limpeza")).isEqualTo(Prioridade.ROTINA);
    }

    @Test
    void naoConfundePalavraParcial() {
        assertThat(detector.possuiSinaisAlarme("quero um traumatologista")).isFalse();
    }

    @Test
    void aceitaListaConfigurada() {
        DetectorSinaisAlarme personalizado = new DetectorSinaisAlarme(List.of("Língua inchada"));

        assertThat(personalizado.possuiSinaisAlarme("minha lingua inchada")).isTrue();
        assertThat(personalizado.possuiSinaisAlarme("rosto inchado")).isFalse();
    }
}
