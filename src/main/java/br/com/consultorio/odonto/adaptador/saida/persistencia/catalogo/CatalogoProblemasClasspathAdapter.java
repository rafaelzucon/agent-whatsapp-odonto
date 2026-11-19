package br.com.consultorio.odonto.adaptador.saida.persistencia.catalogo;

import br.com.consultorio.odonto.adaptador.saida.persistencia.PersistenciaDynamoDb;
import br.com.consultorio.odonto.aplicacao.porta.saida.CatalogoProblemasRepositorio;
import br.com.consultorio.odonto.dominio.modelo.Prioridade;
import br.com.consultorio.odonto.dominio.modelo.ProblemaOdontologico;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Catálogo de problemas lido do mesmo script de migração usado no PostgreSQL ({@value #SCRIPT}), mantendo uma única
 * fonte para os dois modos de persistência. O catálogo é somente leitura, então fica em memória e não ocupa o
 * DynamoDB.
 */
@Repository
@PersistenciaDynamoDb
public class CatalogoProblemasClasspathAdapter implements CatalogoProblemasRepositorio {

    static final String SCRIPT = "db/migration/V2__catalogo_problemas.sql";
    private static final int COLUNAS = 7;

    private final List<ProblemaOdontologico> ativos;
    private final Map<String, ProblemaOdontologico> porCodigo;

    public CatalogoProblemasClasspathAdapter() {
        this(lerScript());
    }

    CatalogoProblemasClasspathAdapter(String script) {
        this.ativos = script.lines().map(String::strip).filter(linha -> linha.startsWith("('"))
                .map(CatalogoProblemasClasspathAdapter::paraDominio)
                .sorted(Comparator.comparing(ProblemaOdontologico::codigo)).toList();
        this.porCodigo = ativos.stream().collect(Collectors.toUnmodifiableMap(ProblemaOdontologico::codigo,
                Function.identity()));
    }

    @Override
    public List<ProblemaOdontologico> listarAtivos() {
        return ativos;
    }

    @Override
    public Optional<ProblemaOdontologico> buscarPorCodigo(String codigo) {
        return Optional.ofNullable(porCodigo.get(codigo));
    }

    private static String lerScript() {
        try (var entrada = new ClassPathResource(SCRIPT).getInputStream()) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler " + SCRIPT + ".", e);
        }
    }

    private static ProblemaOdontologico paraDominio(String linha) {
        List<String> v = valores(linha);
        if (v.size() != COLUNAS) {
            throw new IllegalStateException("Linha do catálogo com " + v.size() + " colunas: " + linha);
        }
        List<String> palavras = v.get(6) == null ? List.of()
                : Arrays.stream(v.get(6).split(";")).map(String::trim).filter(p -> !p.isEmpty()).toList();
        return new ProblemaOdontologico(v.get(0), v.get(1), v.get(2), v.get(3), Prioridade.valueOf(v.get(4)),
                Optional.ofNullable(v.get(5)), palavras);
    }

    /** Valores de uma tupla SQL {@code ('a', 'b''c', NULL)}: textos entre aspas simples ou NULL. */
    static List<String> valores(String linha) {
        List<String> valores = new ArrayList<>();
        int i = linha.indexOf('(') + 1;
        while (i < linha.length()) {
            char c = linha.charAt(i);
            if (c == '\'') {
                StringBuilder texto = new StringBuilder();
                i++;
                while (true) {
                    char atual = linha.charAt(i);
                    if (atual == '\'' && i + 1 < linha.length() && linha.charAt(i + 1) == '\'') {
                        texto.append('\'');
                        i += 2;
                    } else if (atual == '\'') {
                        i++;
                        break;
                    } else {
                        texto.append(atual);
                        i++;
                    }
                }
                valores.add(texto.toString());
            } else if (linha.startsWith("NULL", i)) {
                valores.add(null);
                i += 4;
            } else if (c == ')') {
                break;
            } else {
                i++;
            }
        }
        return valores;
    }
}
