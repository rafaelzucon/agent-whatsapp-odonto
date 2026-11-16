package br.com.consultorio.odonto.adaptador.saida.persistencia;

import br.com.consultorio.odonto.dominio.modelo.DadosConversa;
import br.com.consultorio.odonto.dominio.modelo.DocumentoRg;
import br.com.consultorio.odonto.dominio.modelo.Horario;
import br.com.consultorio.odonto.dominio.modelo.Prioridade;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Representação JSON (antes da cifragem) dos {@link DadosConversa}. Campos simples e anuláveis, para que o
 * domínio não precise conhecer Jackson.
 */
record DadosConversaJson(
        String rgNumero,
        String rgUf,
        String nome,
        String dataNascimento,
        String responsavel,
        Long pacienteId,
        String nomePaciente,
        String numeroProntuario,
        String consultaExistente,
        Long queixaId,
        String problema,
        String prioridade,
        String horarioSugeridoInicio,
        String horarioSugeridoFim) {

    static DadosConversaJson de(DadosConversa d) {
        return new DadosConversaJson(
                d.rg().map(DocumentoRg::numero).orElse(null),
                d.rg().map(rg -> rg.uf().name()).orElse(null),
                d.nome().orElse(null),
                d.dataNascimento().map(LocalDate::toString).orElse(null),
                d.responsavel().orElse(null),
                d.pacienteId().orElse(null),
                d.nomePaciente().orElse(null),
                d.numeroProntuario().orElse(null),
                d.consultaExistente().map(LocalDateTime::toString).orElse(null),
                d.queixaId().orElse(null),
                d.problema().orElse(null),
                d.prioridade().map(Prioridade::name).orElse(null),
                d.horarioSugerido().map(h -> h.inicio().toString()).orElse(null),
                d.horarioSugerido().map(h -> h.fim().toString()).orElse(null));
    }

    DadosConversa paraDominio() {
        Optional<Horario> horario = horarioSugeridoInicio == null || horarioSugeridoFim == null
                ? Optional.empty()
                : Optional.of(new Horario(LocalDateTime.parse(horarioSugeridoInicio),
                LocalDateTime.parse(horarioSugeridoFim)));
        return new DadosConversa(
                rgNumero == null || rgUf == null ? Optional.empty() : Optional.of(DocumentoRg.de(rgNumero, rgUf)),
                Optional.ofNullable(nome),
                Optional.ofNullable(dataNascimento).map(LocalDate::parse),
                Optional.ofNullable(responsavel),
                Optional.ofNullable(pacienteId),
                Optional.ofNullable(nomePaciente),
                Optional.ofNullable(numeroProntuario),
                Optional.ofNullable(consultaExistente).map(LocalDateTime::parse),
                Optional.ofNullable(queixaId),
                Optional.ofNullable(problema),
                Optional.ofNullable(prioridade).map(Prioridade::valueOf),
                horario);
    }
}
