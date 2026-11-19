package br.com.consultorio.odonto.adaptador.saida.persistencia.catalogo;

import br.com.consultorio.odonto.dominio.modelo.Prioridade;
import br.com.consultorio.odonto.dominio.modelo.ProblemaOdontologico;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoProblemasClasspathAdapterTest {

    @Test
    void leOCatalogoDoMesmoScriptDeMigracaoDoPostgreSql() {
        var catalogo = new CatalogoProblemasClasspathAdapter();

        assertThat(catalogo.listarAtivos()).hasSize(75).isSortedAccordingTo(
                (a, b) -> a.codigo().compareTo(b.codigo()));
        assertThat(catalogo.buscarPorCodigo(ProblemaOdontologico.CODIGO_AVALIACAO_GERAL)).hasValueSatisfying(p -> {
            assertThat(p.prioridade()).isEqualTo(Prioridade.ROTINA);
            assertThat(p.cid10()).contains("Z01.2");
            assertThat(p.palavrasChave()).contains("outro assunto", "marcar avaliação");
        });
        assertThat(catalogo.buscarPorCodigo("INEXISTENTE")).isEmpty();
    }

    @Test
    void interpretaAspasEscapadasENulos() {
        assertThat(CatalogoProblemasClasspathAdapter.valores("('A', 'Dente d''água', NULL, 'x;y'),"))
                .containsExactly("A", "Dente d'água", null, "x;y");
    }
}
