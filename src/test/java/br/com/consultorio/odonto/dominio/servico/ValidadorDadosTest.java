package br.com.consultorio.odonto.dominio.servico;

import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;
import br.com.consultorio.odonto.dominio.modelo.DocumentoRg;
import br.com.consultorio.odonto.dominio.modelo.Uf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorDadosTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 16);
    private final ValidadorDados validador = new ValidadorDados();

    @Test
    void rgComPontosETracoENormalizado() {
        assertThat(validador.validarRg("12.345.678-9 SP")).isEqualTo(new DocumentoRg("123456789", Uf.SP));
    }

    @Test
    void rgComXFinalEOrgaoEmissor() {
        assertThat(validador.validarRg("rg 12.345.678-x ssp/rj")).isEqualTo(new DocumentoRg("12345678X", Uf.RJ));
        assertThat(validador.validarRg("MG-12.345.678")).isEqualTo(new DocumentoRg("12345678", Uf.MG));
        assertThat(validador.validarRg("1234567 SSPSP")).isEqualTo(new DocumentoRg("1234567", Uf.SP));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12.345.678-9", "12.345.678-9 XX", "123 SP", "", "abc SP", "123456789012345 SP"})
    void rgSemUfValidaOuComTamanhoErradoEInvalido(String texto) {
        assertThatThrownBy(() -> validador.validarRg(texto)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void nomeCompletoExigeDuasPalavrasEFormataAsIniciais() {
        assertThat(validador.validarNomeCompleto("  maria   DA silva ")).isEqualTo("Maria da Silva");
        assertThat(validador.validarNomeCompleto("José D'Ávila")).isEqualTo("José D'ávila");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Maria", "M 1", "Maria 123", "", "   "})
    void nomeInvalido(String texto) {
        assertThatThrownBy(() -> validador.validarNomeCompleto(texto)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void dataDeNascimentoValida() {
        assertThat(validador.validarDataNascimento("05/03/1990", HOJE)).isEqualTo(LocalDate.of(1990, 3, 5));
        assertThat(validador.validarDataNascimento("5-3-1990", HOJE)).isEqualTo(LocalDate.of(1990, 3, 5));
    }

    @ParameterizedTest
    @ValueSource(strings = {"31/02/1990", "01/01/2030", "01/01/1900", "1990-03-05", "ontem"})
    void dataDeNascimentoInvalida(String texto) {
        assertThatThrownBy(() -> validador.validarDataNascimento(texto, HOJE)).isInstanceOf(DadoInvalidoException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SIM", "sim", "Sim!", "s", "S", "Concordo", "concordo."})
    void respostasSim(String texto) {
        assertThat(validador.interpretarSimNao(texto)).contains(true);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NÃO", "não", "nao", "NAO", "n", "Não."})
    void respostasNao(String texto) {
        assertThat(validador.interpretarSimNao(texto)).contains(false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"talvez", "sexta às 10h", ""})
    void respostasQueNaoSaoSimNemNao(String texto) {
        assertThat(validador.interpretarSimNao(texto)).isEmpty();
    }
}
