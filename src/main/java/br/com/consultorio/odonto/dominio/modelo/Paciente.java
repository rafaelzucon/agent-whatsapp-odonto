package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Paciente do consultório. Só pode ser cadastrado com consentimento LGPD e, se menor de idade,
 * com responsável legal informado.
 */
public final class Paciente {

    public static final int MAIORIDADE = 18;

    private final Long id;
    private final DocumentoRg rg;
    private final String nomeCompleto;
    private final LocalDate dataNascimento;
    private final String responsavelLegal;
    private final Set<WaId> telefones;
    private final boolean consentimentoLgpd;
    private final Instant dataConsentimento;
    private final Instant dataCadastro;

    private Paciente(Long id, DocumentoRg rg, String nomeCompleto, LocalDate dataNascimento, String responsavelLegal,
                     Set<WaId> telefones, boolean consentimentoLgpd, Instant dataConsentimento, Instant dataCadastro) {
        this.id = id;
        this.rg = Objects.requireNonNull(rg, "rg");
        this.nomeCompleto = exigirTexto(nomeCompleto, "nome completo");
        this.dataNascimento = Objects.requireNonNull(dataNascimento, "dataNascimento");
        this.responsavelLegal = responsavelLegal == null || responsavelLegal.isBlank() ? null : responsavelLegal.trim();
        this.telefones = new LinkedHashSet<>(telefones);
        this.consentimentoLgpd = consentimentoLgpd;
        this.dataConsentimento = dataConsentimento;
        this.dataCadastro = Objects.requireNonNull(dataCadastro, "dataCadastro");
    }

    /**
     * Cadastra um paciente novo (primeiro contato pelo WhatsApp).
     *
     * @throws DadoInvalidoException se não houver consentimento ou se faltar responsável para menor de idade
     */
    public static Paciente cadastrar(DocumentoRg rg, String nomeCompleto, LocalDate dataNascimento,
                                     Optional<String> responsavelLegal, WaId telefone, boolean consentimentoLgpd,
                                     Instant agora, LocalDate hoje) {
        if (!consentimentoLgpd) {
            throw new DadoInvalidoException("Sem consentimento LGPD não é possível cadastrar o paciente.");
        }
        if (dataNascimento.isAfter(hoje)) {
            throw new DadoInvalidoException("A data de nascimento não pode estar no futuro.");
        }
        if (idade(dataNascimento, hoje) < MAIORIDADE && responsavelLegal.filter(r -> !r.isBlank()).isEmpty()) {
            throw new DadoInvalidoException("Paciente menor de idade precisa de responsável legal.");
        }
        return new Paciente(null, rg, nomeCompleto, dataNascimento, responsavelLegal.orElse(null),
                Set.of(telefone), true, agora, agora);
    }

    /** Reconstrói um paciente já persistido. */
    public static Paciente restaurar(Long id, DocumentoRg rg, String nomeCompleto, LocalDate dataNascimento,
                                     String responsavelLegal, Set<WaId> telefones, boolean consentimentoLgpd,
                                     Instant dataConsentimento, Instant dataCadastro) {
        return new Paciente(Objects.requireNonNull(id, "id"), rg, nomeCompleto, dataNascimento, responsavelLegal,
                telefones, consentimentoLgpd, dataConsentimento, dataCadastro);
    }

    public static int idade(LocalDate dataNascimento, LocalDate hoje) {
        return Period.between(dataNascimento, hoje).getYears();
    }

    /** Vincula um número de WhatsApp ao paciente. Devolve true se o número ainda não estava vinculado. */
    public boolean vincularTelefone(WaId telefone) {
        return telefones.add(Objects.requireNonNull(telefone, "telefone"));
    }

    public boolean possuiTelefone(WaId telefone) {
        return telefones.contains(telefone);
    }

    public int idade(LocalDate hoje) {
        return idade(dataNascimento, hoje);
    }

    public boolean ehMenorDeIdade(LocalDate hoje) {
        return idade(hoje) < MAIORIDADE;
    }

    public String primeiroNome() {
        return nomeCompleto.split("\\s+")[0];
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DadoInvalidoException("Campo obrigatório: " + campo + ".");
        }
        return valor.trim();
    }

    public Optional<Long> id() {
        return Optional.ofNullable(id);
    }

    public DocumentoRg rg() {
        return rg;
    }

    public String nomeCompleto() {
        return nomeCompleto;
    }

    public LocalDate dataNascimento() {
        return dataNascimento;
    }

    public Optional<String> responsavelLegal() {
        return Optional.ofNullable(responsavelLegal);
    }

    public Set<WaId> telefones() {
        return Collections.unmodifiableSet(telefones);
    }

    public boolean consentimentoLgpd() {
        return consentimentoLgpd;
    }

    public Optional<Instant> dataConsentimento() {
        return Optional.ofNullable(dataConsentimento);
    }

    public Instant dataCadastro() {
        return dataCadastro;
    }
}
