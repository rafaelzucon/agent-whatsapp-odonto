package br.com.consultorio.odonto.dominio.servico;

import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;
import br.com.consultorio.odonto.dominio.excecao.HorarioIndisponivelException;
import br.com.consultorio.odonto.dominio.excecao.MotivoIndisponibilidade;
import br.com.consultorio.odonto.dominio.modelo.BloqueioAgenda;
import br.com.consultorio.odonto.dominio.modelo.Consulta;
import br.com.consultorio.odonto.dominio.modelo.Disponibilidade;
import br.com.consultorio.odonto.dominio.modelo.Horario;
import br.com.consultorio.odonto.dominio.modelo.HorarioFuncionamento;
import br.com.consultorio.odonto.dominio.modelo.Periodo;
import br.com.consultorio.odonto.dominio.modelo.PreferenciaHorario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Regras de disponibilidade da agenda (um dentista, uma cadeira). Única fonte da verdade sobre
 * "este horário pode ser agendado?" e "qual o horário livre mais próximo?".
 * <p>
 * Imutável: é montada com o expediente, os feriados, os bloqueios e as consultas existentes no
 * período de interesse e não guarda estado entre chamadas.
 */
public final class Agenda {

    private final HorarioFuncionamento funcionamento;
    private final Set<LocalDate> feriados;
    private final List<BloqueioAgenda> bloqueios;
    private final List<Consulta> consultas;
    private final int diasBuscaMaxima;

    public Agenda(HorarioFuncionamento funcionamento, Set<LocalDate> feriados, List<BloqueioAgenda> bloqueios,
                  List<Consulta> consultas, int diasBuscaMaxima) {
        this.funcionamento = Objects.requireNonNull(funcionamento, "funcionamento");
        this.feriados = Set.copyOf(feriados);
        this.bloqueios = List.copyOf(bloqueios);
        this.consultas = List.copyOf(consultas);
        if (diasBuscaMaxima < 1) {
            throw new DadoInvalidoException("Os dias de busca máxima devem ser ao menos 1.");
        }
        this.diasBuscaMaxima = diasBuscaMaxima;
    }

    /** Último dia (inclusive) em que se procura horário a partir de hoje. */
    public static LocalDate limiteDeBusca(LocalDateTime agora, int diasBuscaMaxima) {
        return agora.toLocalDate().plusDays(diasBuscaMaxima);
    }

    public Horario horarioQueComecaEm(LocalDateTime inicio) {
        return Horario.de(inicio, funcionamento.duracaoConsulta());
    }

    /** Verifica um início de consulta; vazio significa que o horário está livre. */
    public Optional<MotivoIndisponibilidade> verificar(LocalDateTime inicio, LocalDateTime agora) {
        Horario horario = horarioQueComecaEm(inicio);
        LocalDate data = inicio.toLocalDate();
        LocalTime hora = inicio.toLocalTime();
        if (!inicio.isAfter(agora)) {
            return Optional.of(MotivoIndisponibilidade.NO_PASSADO);
        }
        if (inicio.isBefore(agora.plus(funcionamento.antecedenciaMinima()))) {
            return Optional.of(MotivoIndisponibilidade.ANTECEDENCIA_MINIMA);
        }
        if (data.isAfter(limiteDeBusca(agora, diasBuscaMaxima))) {
            return Optional.of(MotivoIndisponibilidade.ALEM_DO_LIMITE_DE_BUSCA);
        }
        if (feriados.contains(data)) {
            return Optional.of(MotivoIndisponibilidade.FERIADO);
        }
        if (!funcionamento.atendeNo(data.getDayOfWeek())) {
            return Optional.of(MotivoIndisponibilidade.DIA_SEM_ATENDIMENTO);
        }
        if (hora.isBefore(funcionamento.abertura())
                || horario.fim().isAfter(data.atTime(funcionamento.fechamento()))
                || !horario.fim().toLocalDate().equals(data)) {
            return Optional.of(MotivoIndisponibilidade.FORA_DO_EXPEDIENTE);
        }
        if (funcionamento.temAlmoco() && horario.sobrepoe(
                new Horario(data.atTime(funcionamento.almocoInicio()), data.atTime(funcionamento.almocoFim())))) {
            return Optional.of(MotivoIndisponibilidade.INTERVALO_ALMOCO);
        }
        if (!funcionamento.grade(data).contains(inicio)) {
            return Optional.of(MotivoIndisponibilidade.FORA_DA_GRADE);
        }
        if (bloqueios.stream().anyMatch(b -> b.bloqueia(horario))) {
            return Optional.of(MotivoIndisponibilidade.BLOQUEADO);
        }
        if (consultas.stream().anyMatch(c -> c.ocupa(horario))) {
            return Optional.of(MotivoIndisponibilidade.OCUPADO);
        }
        return Optional.empty();
    }

    public boolean estaLivre(LocalDateTime inicio, LocalDateTime agora) {
        return verificar(inicio, agora).isEmpty();
    }

    /**
     * Garante que o horário pode ser agendado.
     *
     * @throws HorarioIndisponivelException com o motivo, se não puder
     */
    public Horario validar(LocalDateTime inicio, LocalDateTime agora) {
        verificar(inicio, agora).ifPresent(motivo -> {
            throw new HorarioIndisponivelException(motivo);
        });
        return horarioQueComecaEm(inicio);
    }

    /** Primeiro horário livre com início igual ou posterior a {@code aPartirDe}, até o limite de busca. */
    public Optional<Horario> proximoLivre(LocalDateTime aPartirDe, LocalDateTime agora) {
        LocalDate limite = limiteDeBusca(agora, diasBuscaMaxima);
        for (LocalDate dia = aPartirDe.toLocalDate(); !dia.isAfter(limite); dia = dia.plusDays(1)) {
            for (LocalDateTime inicio : funcionamento.grade(dia)) {
                if (!inicio.isBefore(aPartirDe) && estaLivre(inicio, agora)) {
                    return Optional.of(horarioQueComecaEm(inicio));
                }
            }
        }
        return Optional.empty();
    }

    /** Horário livre mais próximo antes de {@code antesDe}, no mesmo dia e no futuro. */
    public Optional<Horario> anteriorLivreNoMesmoDia(LocalDateTime antesDe, LocalDateTime agora) {
        List<LocalDateTime> grade = new ArrayList<>(funcionamento.grade(antesDe.toLocalDate()));
        for (int i = grade.size() - 1; i >= 0; i--) {
            LocalDateTime inicio = grade.get(i);
            if (inicio.isBefore(antesDe) && estaLivre(inicio, agora)) {
                return Optional.of(horarioQueComecaEm(inicio));
            }
        }
        return Optional.empty();
    }

    public Optional<Horario> primeiroLivreNoDia(LocalDate dia, LocalDateTime agora) {
        return funcionamento.grade(dia).stream()
                .filter(inicio -> estaLivre(inicio, agora))
                .findFirst()
                .map(this::horarioQueComecaEm);
    }

    public Optional<Horario> primeiroLivreNoPeriodo(LocalDate dia, Periodo periodo, LocalDateTime agora) {
        return funcionamento.grade(dia).stream()
                .filter(inicio -> periodo.contem(inicio.toLocalTime()))
                .filter(inicio -> estaLivre(inicio, agora))
                .findFirst()
                .map(this::horarioQueComecaEm);
    }

    /** Primeiro horário livre a partir de agora (usado na oferta para emergências). */
    public Disponibilidade primeiroLivre(LocalDateTime agora) {
        return proximoLivre(agora, agora)
                .<Disponibilidade>map(h -> new Disponibilidade.HorarioSugerido(h, false))
                .orElseGet(Disponibilidade.SemHorarioLivre::new);
    }

    /**
     * Resolve a preferência do paciente:
     * <ul>
     *   <li>data e hora: o horário exato, se livre; senão o livre mais próximo depois dele (ou, se não houver,
     *   o mais próximo antes, no mesmo dia e no futuro);</li>
     *   <li>só a hora: hoje nessa hora (ou amanhã, se já passou), com a mesma regra;</li>
     *   <li>data e período: o primeiro livre nesse período do dia; senão o próximo livre depois;</li>
     *   <li>só a data: o primeiro livre do dia; senão o próximo livre depois;</li>
     *   <li>só o período: o primeiro livre nesse período a partir de hoje.</li>
     * </ul>
     */
    public Disponibilidade resolver(PreferenciaHorario preferencia, LocalDateTime agora) {
        Optional<LocalTime> hora = preferencia.hora();
        if (hora.isPresent()) {
            LocalDate data = preferencia.data().orElseGet(() -> proximaDataParaHora(hora.get(), agora));
            return resolverHorarioExato(data.atTime(hora.get()), agora);
        }
        if (preferencia.data().isPresent()) {
            LocalDate data = preferencia.data().get();
            Optional<Horario> noDia = preferencia.periodo()
                    .map(p -> primeiroLivreNoPeriodo(data, p, agora))
                    .orElseGet(() -> primeiroLivreNoDia(data, agora));
            if (noDia.isPresent()) {
                return new Disponibilidade.HorarioSugerido(noDia.get(), false);
            }
            LocalDateTime pontoDePartida = preferencia.periodo()
                    .map(p -> data.atTime(p.inicio()))
                    .orElseGet(data::atStartOfDay);
            return sugerirAPartirDe(pontoDePartida, agora, true);
        }
        Periodo periodo = preferencia.periodo().orElseThrow();
        LocalDate limite = limiteDeBusca(agora, diasBuscaMaxima);
        for (LocalDate dia = agora.toLocalDate(); !dia.isAfter(limite); dia = dia.plusDays(1)) {
            Optional<Horario> livre = primeiroLivreNoPeriodo(dia, periodo, agora);
            if (livre.isPresent()) {
                return new Disponibilidade.HorarioSugerido(livre.get(), false);
            }
        }
        return sugerirAPartirDe(agora, agora, true);
    }

    private Disponibilidade resolverHorarioExato(LocalDateTime inicio, LocalDateTime agora) {
        if (estaLivre(inicio, agora)) {
            return new Disponibilidade.HorarioDisponivel(horarioQueComecaEm(inicio));
        }
        LocalDateTime pontoDePartida = inicio.isAfter(agora) ? inicio : agora;
        Optional<Horario> depois = proximoLivre(pontoDePartida, agora);
        if (depois.isPresent()) {
            return new Disponibilidade.HorarioSugerido(depois.get(), true);
        }
        return anteriorLivreNoMesmoDia(inicio, agora)
                .<Disponibilidade>map(h -> new Disponibilidade.HorarioSugerido(h, true))
                .orElseGet(Disponibilidade.SemHorarioLivre::new);
    }

    private Disponibilidade sugerirAPartirDe(LocalDateTime ponto, LocalDateTime agora, boolean pedidoIndisponivel) {
        LocalDateTime pontoDePartida = ponto.isAfter(agora) ? ponto : agora;
        return proximoLivre(pontoDePartida, agora)
                .<Disponibilidade>map(h -> new Disponibilidade.HorarioSugerido(h, pedidoIndisponivel))
                .orElseGet(Disponibilidade.SemHorarioLivre::new);
    }

    private static LocalDate proximaDataParaHora(LocalTime hora, LocalDateTime agora) {
        return hora.isAfter(agora.toLocalTime()) ? agora.toLocalDate() : agora.toLocalDate().plusDays(1);
    }

    /** Horários livres da grade entre as datas (inclusive), para a consulta da agenda pela recepção. */
    public List<Horario> horariosLivres(LocalDate inicio, LocalDate fim, LocalDateTime agora) {
        List<Horario> livres = new ArrayList<>();
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            funcionamento.grade(dia).stream()
                    .filter(h -> estaLivre(h, agora))
                    .map(this::horarioQueComecaEm)
                    .forEach(livres::add);
        }
        return livres;
    }
}
