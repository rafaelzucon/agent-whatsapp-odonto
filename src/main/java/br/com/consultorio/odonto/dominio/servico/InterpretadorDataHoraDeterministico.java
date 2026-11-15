package br.com.consultorio.odonto.dominio.servico;

import br.com.consultorio.odonto.dominio.modelo.Periodo;
import br.com.consultorio.odonto.dominio.modelo.PreferenciaHorario;

import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reconhece, sem IA, formatos comuns de data e hora: "25/11 às 10h", "25/11/2026 10:30", "amanhã 14h",
 * "hoje 16h", "depois de amanhã de manhã". Se o texto tiver expressões que exigem interpretação
 * (dia da semana, "próxima", "dia 25"), devolve vazio para que a IA seja consultada.
 */
public final class InterpretadorDataHoraDeterministico {

    private static final Pattern DATA = Pattern.compile("\\b(\\d{1,2})/(\\d{1,2})(?:/(\\d{2}|\\d{4}))?\\b");
    private static final Pattern HORA = Pattern.compile(
            "(?:\\bas\\s+(\\d{1,2})(?:[:h](\\d{2}))?\\b)|(?:\\b(\\d{1,2})\\s*(?::|h|hs|horas?)\\s*(\\d{2})?\\b)");
    private static final Pattern PRECISA_DE_IA = Pattern.compile(
            "\\b(segunda|terca|quarta|quinta|sexta|sabado|domingo|semana|mes|proxim[oa]|dia\\s+\\d|"
                    + "meio dia|meia noite|daqui)\\b");
    private static final Pattern DEPOIS_DE_AMANHA = Pattern.compile("\\bdepois de amanha\\b");
    private static final Pattern AMANHA = Pattern.compile("\\bamanha\\b");
    private static final Pattern HOJE = Pattern.compile("\\bhoje\\b");
    private static final Pattern MANHA = Pattern.compile("\\bmanha\\b");
    private static final Pattern TARDE = Pattern.compile("\\btarde\\b");
    private static final Pattern NOITE = Pattern.compile("\\bnoite\\b");

    public Optional<PreferenciaHorario> interpretar(String texto, LocalDateTime agora) {
        String t = simplificar(texto);
        if (t.isBlank() || PRECISA_DE_IA.matcher(t).find()) {
            return Optional.empty();
        }
        Optional<LocalDate> data = extrairData(t, agora.toLocalDate());
        if (data.isEmpty()) {
            return Optional.empty();
        }
        String semData = DATA.matcher(t).replaceAll(" ");
        Optional<LocalTime> hora = extrairHora(semData);
        Optional<Periodo> periodo = hora.isPresent() ? Optional.empty() : extrairPeriodo(semData);
        return Optional.of(new PreferenciaHorario(data, hora, periodo));
    }

    private static String simplificar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/: ]", " ").replaceAll("\\s+", " ").trim();
    }

    private static Optional<LocalDate> extrairData(String t, LocalDate hoje) {
        Matcher m = DATA.matcher(t);
        if (m.find()) {
            return dataExplicita(m, hoje);
        }
        if (DEPOIS_DE_AMANHA.matcher(t).find()) {
            return Optional.of(hoje.plusDays(2));
        }
        if (AMANHA.matcher(t).find()) {
            return Optional.of(hoje.plusDays(1));
        }
        if (HOJE.matcher(t).find()) {
            return Optional.of(hoje);
        }
        return Optional.empty();
    }

    private static Optional<LocalDate> dataExplicita(Matcher m, LocalDate hoje) {
        try {
            int dia = Integer.parseInt(m.group(1));
            int mes = Integer.parseInt(m.group(2));
            if (m.group(3) != null) {
                int ano = Integer.parseInt(m.group(3));
                return Optional.of(LocalDate.of(ano < 100 ? 2000 + ano : ano, mes, dia));
            }
            LocalDate data = LocalDate.of(hoje.getYear(), mes, dia);
            return Optional.of(data.isBefore(hoje) ? data.plusYears(1) : data);
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }

    private static Optional<LocalTime> extrairHora(String t) {
        Matcher m = HORA.matcher(t);
        while (m.find()) {
            String h = m.group(1) != null ? m.group(1) : m.group(3);
            String min = m.group(1) != null ? m.group(2) : m.group(4);
            int hora = Integer.parseInt(h);
            int minuto = min == null ? 0 : Integer.parseInt(min);
            if (hora <= 23 && minuto <= 59) {
                return Optional.of(LocalTime.of(hora, minuto));
            }
        }
        return Optional.empty();
    }

    private static Optional<Periodo> extrairPeriodo(String t) {
        if (MANHA.matcher(t).find()) {
            return Optional.of(Periodo.MANHA);
        }
        if (TARDE.matcher(t).find()) {
            return Optional.of(Periodo.TARDE);
        }
        if (NOITE.matcher(t).find()) {
            return Optional.of(Periodo.NOITE);
        }
        return Optional.empty();
    }
}
