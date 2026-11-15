package br.com.consultorio.odonto.dominio.servico;

import br.com.consultorio.odonto.dominio.modelo.ClassificacaoQueixa;
import br.com.consultorio.odonto.dominio.modelo.OrigemClassificacao;
import br.com.consultorio.odonto.dominio.modelo.Prioridade;
import br.com.consultorio.odonto.dominio.modelo.ProblemaOdontologico;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificadorPorPalavraChaveTest {

    static final ProblemaOdontologico CARIE = problema("CARIE", Prioridade.ROTINA, "cárie", "buraco no dente", "dente preto");
    static final ProblemaOdontologico PULPITE = problema("PULPITE", Prioridade.URGENCIA, "dor de dente", "dor latejante",
            "dói com gelado");
    static final ProblemaOdontologico LIMPEZA = problema("PROFILAXIA", Prioridade.ROTINA, "limpeza", "tirar tártaro");
    static final ProblemaOdontologico GERAL = problema(ProblemaOdontologico.CODIGO_AVALIACAO_GERAL, Prioridade.ROTINA,
            "avaliação", "dor de dente");
    static final List<ProblemaOdontologico> CATALOGO = List.of(CARIE, PULPITE, LIMPEZA, GERAL);

    private final ClassificadorPorPalavraChave classificador = new ClassificadorPorPalavraChave();

    static ProblemaOdontologico problema(String codigo, Prioridade prioridade, String... palavras) {
        return new ProblemaOdontologico(codigo, codigo, "", "Clínica Geral", prioridade, Optional.empty(), List.of(palavras));
    }

    @Test
    void escolheOItemComMaisPalavrasChaveEncontradas() {
        Optional<ClassificacaoQueixa> resultado = classificador.classificar(
                "Estou com uma DOR DE DENTE latejante, dói com gelado", CATALOGO);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().problema()).isEqualTo(PULPITE);
        assertThat(resultado.get().origem()).isEqualTo(OrigemClassificacao.PALAVRA_CHAVE);
        assertThat(resultado.get().prioridade()).isEqualTo(Prioridade.URGENCIA);
        assertThat(resultado.get().confianca()).isBetween(0.5, 0.9);
    }

    @Test
    void ignoraAcentosEPontuacao() {
        assertThat(classificador.classificar("tenho um buraco no dente!!", CATALOGO))
                .map(ClassificacaoQueixa::problema).contains(CARIE);
        assertThat(classificador.classificar("quero fazer LIMPEZA", CATALOGO))
                .map(ClassificacaoQueixa::problema).contains(LIMPEZA);
    }

    @Test
    void semCorrespondenciaDevolveVazioENuncaUsaAvaliacaoGeral() {
        assertThat(classificador.classificar("meu aparelho soltou", CATALOGO)).isEmpty();
        assertThat(classificador.classificar("", CATALOGO)).isEmpty();
    }
}
