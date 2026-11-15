package br.com.consultorio.odonto.aplicacao.porta.entrada;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public record PacienteVisao(long id, String nomeCompleto, String rg, String ufRg, LocalDate dataNascimento, int idade,
                            Optional<String> responsavelLegal, List<String> telefones,
                            Optional<String> numeroProntuario, boolean consentimentoLgpd,
                            Optional<Instant> dataConsentimento, Instant dataCadastro) {
}
