package br.com.consultorio.odonto.dominio.servico;

import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;
import br.com.consultorio.odonto.dominio.modelo.DocumentoRg;
import br.com.consultorio.odonto.dominio.modelo.Paciente;
import br.com.consultorio.odonto.dominio.modelo.Uf;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validação dos dados digitados pelo paciente na conversa. Sem estado.
 */
public final class ValidadorDados {

    public static final int IDADE_MAXIMA = 120;

    private static final Pattern NUMERO_RG = Pattern.compile("\\d[\\d.\\-\\s/]*\\d(?:\\s*-?\\s*[xX](?![a-zA-Z]))?|\\d");
    private static final Pattern PALAVRA_LETRAS = Pattern.compile("\\p{L}+");
    private static final Pattern NOME_PERMITIDO = Pattern.compile("[\\p{L} .'\\-]+");
    private static final Pattern DATA = Pattern.compile("(\\d{1,2})[/.\\-](\\d{1,2})[/.\\-](\\d{4})");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("d/M/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Set<String> PARTICULAS = Set.of("da", "de", "do", "das", "dos", "e");
    private static final Set<String> SIM = Set.of("sim", "s", "concordo", "sim concordo", "aceito", "sim aceito",
            "ok", "pode", "pode sim", "claro", "sim pode", "confirmo", "sim confirmo");
    private static final Set<String> NAO = Set.of("nao", "n", "nao concordo", "nao aceito", "nao obrigado",
            "nao quero");

    /**
     * Extrai e valida o RG e a UF do órgão emissor (ex.: "12.345.678-9 SP", "MG-12.345.678", "1234567X ssp/rj").
     */
    public DocumentoRg validarRg(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new DadoInvalidoException("Informe o RG e a UF.");
        }
        Uf uf = ultimaUfCitada(texto)
                .orElseThrow(() -> new DadoInvalidoException("Informe também a UF do órgão emissor (ex.: SP)."));
        String numero = maiorSequenciaNumerica(texto)
                .orElseThrow(() -> new DadoInvalidoException("Não encontrei o número do RG."));
        return new DocumentoRg(numero, uf);
    }

    private static Optional<Uf> ultimaUfCitada(String texto) {
        Matcher palavras = PALAVRA_LETRAS.matcher(texto.toUpperCase(Locale.ROOT));
        Optional<Uf> encontrada = Optional.empty();
        while (palavras.find()) {
            String palavra = palavras.group();
            if (palavra.length() == 2) {
                Optional<Uf> uf = Uf.buscar(palavra);
                if (uf.isPresent()) {
                    encontrada = uf;
                }
            }
        }
        return encontrada.or(() -> ufColadaNoOrgao(texto));
    }

    /** Aceita formatos como "SSPSP" ou "SSPRJ", em que a UF vem colada à sigla do órgão. */
    private static Optional<Uf> ufColadaNoOrgao(String texto) {
        Matcher m = Pattern.compile("\\bSSP([A-Z]{2})\\b").matcher(texto.toUpperCase(Locale.ROOT));
        return m.find() ? Uf.buscar(m.group(1)) : Optional.empty();
    }

    private static Optional<String> maiorSequenciaNumerica(String texto) {
        Matcher m = NUMERO_RG.matcher(texto);
        List<String> candidatos = new ArrayList<>();
        while (m.find()) {
            candidatos.add(m.group());
        }
        return candidatos.stream().max(Comparator.comparingInt(c -> c.replaceAll("[^\\dxX]", "").length()));
    }

    /**
     * Nome completo: pelo menos duas palavras com letras. Devolve o nome com espaços normalizados e
     * iniciais maiúsculas (exceto partículas como "da" e "dos").
     */
    public String validarNomeCompleto(String texto) {
        if (texto == null) {
            throw new DadoInvalidoException("Informe o nome completo.");
        }
        String nome = texto.trim().replaceAll("\\s+", " ");
        if (nome.length() < 5 || nome.length() > 150 || !NOME_PERMITIDO.matcher(nome).matches()) {
            throw new DadoInvalidoException("Nome inválido: use apenas letras.");
        }
        String[] partes = nome.split(" ");
        long palavrasComLetras = Arrays.stream(partes).filter(p -> PALAVRA_LETRAS.matcher(p).find()).count();
        if (palavrasComLetras < 2) {
            throw new DadoInvalidoException("Informe nome e sobrenome.");
        }
        return capitalizar(partes);
    }

    private static String capitalizar(String[] partes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partes.length; i++) {
            String parte = partes[i].toLowerCase(Locale.ROOT);
            if (i > 0) {
                sb.append(' ');
            }
            if (i > 0 && PARTICULAS.contains(parte)) {
                sb.append(parte);
            } else {
                sb.append(Character.toUpperCase(parte.charAt(0))).append(parte.substring(1));
            }
        }
        return sb.toString();
    }

    /** Data de nascimento no formato dd/mm/aaaa, não futura e com idade de até 120 anos. */
    public LocalDate validarDataNascimento(String texto, LocalDate hoje) {
        Matcher m = DATA.matcher(texto == null ? "" : texto.trim());
        if (!m.find()) {
            throw new DadoInvalidoException("Use o formato dd/mm/aaaa.");
        }
        LocalDate data;
        try {
            data = LocalDate.parse(m.group(1) + "/" + m.group(2) + "/" + m.group(3), FORMATO_DATA);
        } catch (DateTimeParseException e) {
            throw new DadoInvalidoException("Data inexistente.");
        }
        if (data.isAfter(hoje)) {
            throw new DadoInvalidoException("A data de nascimento não pode estar no futuro.");
        }
        if (Paciente.idade(data, hoje) > IDADE_MAXIMA) {
            throw new DadoInvalidoException("Idade acima de " + IDADE_MAXIMA + " anos.");
        }
        return data;
    }

    /**
     * Interpreta respostas SIM/NÃO sem diferenciar acentos e maiúsculas.
     *
     * @return true para SIM, false para NÃO, vazio se a resposta não for nenhum dos dois
     */
    public Optional<Boolean> interpretarSimNao(String texto) {
        String normalizado = NormalizadorTexto.normalizar(texto);
        if (SIM.contains(normalizado)) {
            return Optional.of(true);
        }
        if (NAO.contains(normalizado)) {
            return Optional.of(false);
        }
        return Optional.empty();
    }
}
