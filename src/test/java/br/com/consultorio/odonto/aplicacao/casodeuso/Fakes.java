package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.excecao.FalhaIntegracaoException;
import br.com.consultorio.odonto.aplicacao.excecao.MensagemDuplicadaException;
import br.com.consultorio.odonto.aplicacao.porta.saida.AlertaConsultorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.AvisoAgendamento;
import br.com.consultorio.odonto.aplicacao.porta.saida.BloqueioAgendaRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.CanalNotificacaoConsultorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.CatalogoProblemasRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.CifradorDocumento;
import br.com.consultorio.odonto.aplicacao.porta.saida.ClassificacaoIA;
import br.com.consultorio.odonto.aplicacao.porta.saida.ClassificadorProblemaIA;
import br.com.consultorio.odonto.aplicacao.porta.saida.ConsultaRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ConversaRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.EnviadorWhatsApp;
import br.com.consultorio.odonto.aplicacao.porta.saida.ExecutorTransacional;
import br.com.consultorio.odonto.aplicacao.porta.saida.InterpretadorDataHoraIA;
import br.com.consultorio.odonto.aplicacao.porta.saida.MensagemRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.MidiaBaixada;
import br.com.consultorio.odonto.aplicacao.porta.saida.PacienteRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ProntuarioRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ProvedorMidia;
import br.com.consultorio.odonto.aplicacao.porta.saida.PublicadorEventos;
import br.com.consultorio.odonto.aplicacao.porta.saida.Relogio;
import br.com.consultorio.odonto.aplicacao.porta.saida.RetencaoConversas;
import br.com.consultorio.odonto.aplicacao.porta.saida.RetencaoMensagens;
import br.com.consultorio.odonto.aplicacao.porta.saida.TranscritorAudio;
import br.com.consultorio.odonto.dominio.excecao.HorarioIndisponivelException;
import br.com.consultorio.odonto.dominio.excecao.MotivoIndisponibilidade;
import br.com.consultorio.odonto.dominio.modelo.BloqueioAgenda;
import br.com.consultorio.odonto.dominio.modelo.Consulta;
import br.com.consultorio.odonto.dominio.modelo.Conversa;
import br.com.consultorio.odonto.dominio.modelo.DocumentoRg;
import br.com.consultorio.odonto.dominio.modelo.EtapaConversa;
import br.com.consultorio.odonto.dominio.modelo.Mensagem;
import br.com.consultorio.odonto.dominio.modelo.Paciente;
import br.com.consultorio.odonto.dominio.modelo.PreferenciaHorario;
import br.com.consultorio.odonto.dominio.modelo.Prioridade;
import br.com.consultorio.odonto.dominio.modelo.ProblemaOdontologico;
import br.com.consultorio.odonto.dominio.modelo.Prontuario;
import br.com.consultorio.odonto.dominio.modelo.QueixaRelatada;
import br.com.consultorio.odonto.dominio.modelo.WaId;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

/** Implementações em memória das portas de saída, para testar os casos de uso sem banco nem APIs externas. */
final class Fakes {

    static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    /** Segunda-feira, 16/11/2026, 09:00 em São Paulo. */
    static final Instant AGORA = Instant.parse("2026-11-16T12:00:00Z");

    private Fakes() {
    }

    static final class RelogioFixo implements Relogio {
        private Instant agora = AGORA;

        @Override
        public Instant agora() {
            return agora;
        }

        @Override
        public ZoneId fuso() {
            return FUSO;
        }

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }
    }

    static final class TransacaoDireta implements ExecutorTransacional {
        @Override
        public <T> T executar(java.util.function.Supplier<T> operacao) {
            return operacao.get();
        }
    }

    static final class CifradorSimples implements CifradorDocumento {
        @Override
        public String hash(DocumentoRg rg) {
            return "hash:" + rg.chaveCanonica();
        }
    }

    static final class PacientesEmMemoria implements PacienteRepositorio {
        private final Map<Long, Paciente> porId = new LinkedHashMap<>();
        private final AtomicLong sequencia = new AtomicLong();
        private final CifradorDocumento cifrador = new CifradorSimples();

        @Override
        public Paciente salvar(Paciente paciente) {
            Paciente salvo = paciente.id().isPresent() ? paciente : Paciente.restaurar(sequencia.incrementAndGet(),
                    paciente.rg(), paciente.nomeCompleto(), paciente.dataNascimento(),
                    paciente.responsavelLegal().orElse(null), paciente.telefones(), paciente.consentimentoLgpd(),
                    paciente.dataConsentimento().orElse(null), paciente.dataCadastro());
            porId.put(salvo.id().orElseThrow(), salvo);
            return salvo;
        }

        @Override
        public Optional<Paciente> buscarPorId(long id) {
            return Optional.ofNullable(porId.get(id));
        }

        @Override
        public Optional<Paciente> buscarPorRgHash(String rgHash) {
            return porId.values().stream().filter(p -> cifrador.hash(p.rg()).equals(rgHash)).findFirst();
        }

        @Override
        public List<Paciente> listarTodos() {
            return List.copyOf(porId.values());
        }
    }

    static final class ProntuariosEmMemoria implements ProntuarioRepositorio {
        private final Map<Long, Prontuario> porPaciente = new LinkedHashMap<>();
        private final AtomicLong sequencia = new AtomicLong();
        private final AtomicLong idsQueixa = new AtomicLong(100);
        private final AtomicLong numeros = new AtomicLong();

        @Override
        public Prontuario salvar(Prontuario prontuario) {
            List<QueixaRelatada> queixas = prontuario.queixas().stream()
                    .map(q -> q.id().isPresent() ? q : QueixaRelatada.restaurar(idsQueixa.incrementAndGet(),
                            q.textoOriginal(), q.problema(), q.prioridade(), q.confianca(), q.origem(),
                            q.dataRegistro(), q.consultaId().orElse(null)))
                    .toList();
            Prontuario salvo = Prontuario.restaurar(prontuario.id().orElseGet(sequencia::incrementAndGet),
                    prontuario.pacienteId(), prontuario.numero(), prontuario.dataAbertura(), new ArrayList<>(queixas));
            porPaciente.put(salvo.pacienteId(), salvo);
            return salvo;
        }

        @Override
        public long proximoSequencial() {
            return numeros.incrementAndGet();
        }

        @Override
        public Optional<Prontuario> buscarPorPacienteId(long pacienteId) {
            return Optional.ofNullable(porPaciente.get(pacienteId));
        }

        @Override
        public Optional<Prontuario> buscarPorQueixaId(long queixaId) {
            return porPaciente.values().stream().filter(p -> p.queixa(queixaId).isPresent()).findFirst();
        }
    }

    static final class ConsultasEmMemoria implements ConsultaRepositorio {
        final Map<Long, Consulta> porId = new LinkedHashMap<>();
        private final AtomicLong sequencia = new AtomicLong();
        /** Simula outro paciente gravando no mesmo horário entre a verificação e a gravação. */
        Optional<Consulta> concorrente = Optional.empty();

        @Override
        public Consulta salvar(Consulta consulta) {
            concorrente.ifPresent(c -> {
                concorrente = Optional.empty();
                salvar(c);
            });
            boolean ocupado = porId.values().stream().anyMatch(c -> c.estaAgendada() && consulta.estaAgendada()
                    && !c.id().equals(consulta.id()) && c.horario().inicio().equals(consulta.horario().inicio()));
            if (ocupado) {
                throw new HorarioIndisponivelException(MotivoIndisponibilidade.OCUPADO);
            }
            Consulta salva = consulta.id().isPresent() ? consulta : Consulta.restaurar(sequencia.incrementAndGet(),
                    consulta.pacienteId(), consulta.queixaId().orElse(null), consulta.horario(), consulta.status(),
                    consulta.criadaEm());
            porId.put(salva.id().orElseThrow(), salva);
            return salva;
        }

        @Override
        public Optional<Consulta> buscarPorId(long id) {
            return Optional.ofNullable(porId.get(id));
        }

        @Override
        public List<Consulta> listarAgendadasEntre(LocalDateTime inicio, LocalDateTime fim) {
            return listarEntre(inicio, fim).stream().filter(Consulta::estaAgendada).toList();
        }

        @Override
        public List<Consulta> listarEntre(LocalDateTime inicio, LocalDateTime fim) {
            return porId.values().stream()
                    .filter(c -> !c.horario().inicio().isBefore(inicio) && c.horario().inicio().isBefore(fim))
                    .sorted(Comparator.comparing(c -> c.horario().inicio()))
                    .toList();
        }

        @Override
        public List<Consulta> listarPorPaciente(long pacienteId) {
            return porId.values().stream().filter(c -> c.pacienteId() == pacienteId).toList();
        }

        @Override
        public Optional<Consulta> proximaAgendadaDoPaciente(long pacienteId, LocalDateTime agora) {
            return listarPorPaciente(pacienteId).stream()
                    .filter(c -> c.estaAgendada() && c.horario().inicio().isAfter(agora))
                    .min(Comparator.comparing(c -> c.horario().inicio()));
        }
    }

    static final class BloqueiosEmMemoria implements BloqueioAgendaRepositorio {
        private final Map<Long, BloqueioAgenda> porId = new LinkedHashMap<>();
        private final AtomicLong sequencia = new AtomicLong();

        @Override
        public BloqueioAgenda salvar(BloqueioAgenda bloqueio) {
            BloqueioAgenda salvo = new BloqueioAgenda(Optional.of(sequencia.incrementAndGet()), bloqueio.periodo(),
                    bloqueio.motivo());
            porId.put(salvo.id().orElseThrow(), salvo);
            return salvo;
        }

        @Override
        public boolean remover(long id) {
            return porId.remove(id) != null;
        }

        @Override
        public List<BloqueioAgenda> listarEntre(LocalDateTime inicio, LocalDateTime fim) {
            return porId.values().stream()
                    .filter(b -> b.periodo().inicio().isBefore(fim) && b.periodo().fim().isAfter(inicio))
                    .toList();
        }
    }

    static final class ConversasEmMemoria implements ConversaRepositorio, RetencaoConversas {
        final Map<WaId, Conversa> porWaId = new LinkedHashMap<>();
        private final AtomicLong sequencia = new AtomicLong();

        @Override
        public Optional<Conversa> buscarPorWaId(WaId waId) {
            return Optional.ofNullable(porWaId.get(waId));
        }

        @Override
        public Conversa salvar(Conversa conversa) {
            Conversa salva = Conversa.restaurar(conversa.id().orElseGet(sequencia::incrementAndGet), conversa.waId(),
                    conversa.etapa(), conversa.dados(), conversa.tentativasInvalidas(), conversa.ultimaInteracao(),
                    conversa.iniciadaEm());
            porWaId.put(salva.waId(), salva);
            return salva;
        }

        @Override
        public int removerInativasDesde(Instant limite, Set<EtapaConversa> etapas) {
            List<WaId> remover = porWaId.values().stream()
                    .filter(c -> c.ultimaInteracao().isBefore(limite) && etapas.contains(c.etapa()))
                    .map(Conversa::waId)
                    .toList();
            remover.forEach(porWaId::remove);
            return remover.size();
        }
    }

    static final class MensagensEmMemoria implements MensagemRepositorio, RetencaoMensagens {
        final Map<Long, Mensagem> porId = new LinkedHashMap<>();
        private final AtomicLong sequencia = new AtomicLong();

        @Override
        public Mensagem salvar(Mensagem mensagem) {
            if (mensagem.id().isPresent()) {
                porId.put(mensagem.id().get(), mensagem);
                return mensagem;
            }
            if (mensagem.waMessageId().filter(this::existePorWaMessageId).isPresent()) {
                throw new MensagemDuplicadaException("duplicada");
            }
            Mensagem salva = Mensagem.restaurar(sequencia.incrementAndGet(), mensagem.waMessageId().orElse(null),
                    mensagem.waId(), mensagem.direcao(), mensagem.tipo(), mensagem.texto().orElse(null),
                    mensagem.midiaId().orElse(null), mensagem.automatica(), mensagem.criadaEm());
            porId.put(salva.id().orElseThrow(), salva);
            return salva;
        }

        @Override
        public boolean existePorWaMessageId(String waMessageId) {
            return porId.values().stream().anyMatch(m -> m.waMessageId().filter(waMessageId::equals).isPresent());
        }

        @Override
        public Optional<Mensagem> buscarPorId(long id) {
            return Optional.ofNullable(porId.get(id));
        }

        @Override
        public int removerAnterioresA(Instant limite) {
            List<Long> ids = porId.values().stream().filter(m -> m.criadaEm().isBefore(limite))
                    .map(m -> m.id().orElseThrow()).toList();
            ids.forEach(porId::remove);
            return ids.size();
        }
    }

    static final class CatalogoEmMemoria implements CatalogoProblemasRepositorio {
        static final ProblemaOdontologico DOR_DE_DENTE = new ProblemaOdontologico("DOR_DE_DENTE", "Dor de dente",
                "Dor em um dente", "Clínica Geral", Prioridade.URGENCIA, Optional.of("K08.8"),
                List.of("dor de dente", "dente doendo"));
        static final ProblemaOdontologico CARIE = new ProblemaOdontologico("CARIE", "Cárie dentária", "Cárie",
                "Dentística", Prioridade.ROTINA, Optional.of("K02.1"), List.of("carie", "buraco no dente"));
        static final ProblemaOdontologico LIMPEZA = new ProblemaOdontologico("LIMPEZA", "Limpeza", "Profilaxia",
                "Clínica Geral", Prioridade.ROTINA, Optional.empty(), List.of("limpeza", "tartaro"));
        static final ProblemaOdontologico AVALIACAO_GERAL = new ProblemaOdontologico(
                ProblemaOdontologico.CODIGO_AVALIACAO_GERAL, "Avaliação geral", "Avaliação", "Clínica Geral",
                Prioridade.ROTINA, Optional.of("Z01.2"), List.of("avaliacao"));

        @Override
        public List<ProblemaOdontologico> listarAtivos() {
            return List.of(DOR_DE_DENTE, CARIE, LIMPEZA, AVALIACAO_GERAL);
        }

        @Override
        public Optional<ProblemaOdontologico> buscarPorCodigo(String codigo) {
            return listarAtivos().stream().filter(p -> p.codigo().equals(codigo)).findFirst();
        }
    }

    /** IA de classificação programável; sem resposta configurada simula a API fora do ar. */
    static final class ClassificadorIAFake implements ClassificadorProblemaIA {
        BiFunction<String, List<ProblemaOdontologico>, ClassificacaoIA> resposta;
        int chamadas;

        @Override
        public ClassificacaoIA classificar(String queixa, List<ProblemaOdontologico> catalogo) {
            chamadas++;
            if (resposta == null) {
                throw new FalhaIntegracaoException("IA indisponível");
            }
            return resposta.apply(queixa, catalogo);
        }

        void responder(String codigo, double confianca) {
            resposta = (q, c) -> new ClassificacaoIA(codigo, confianca, List.of(), false, Optional.empty());
        }
    }

    static final class InterpretadorIAFake implements InterpretadorDataHoraIA {
        Optional<PreferenciaHorario> resposta = Optional.empty();
        boolean falhar;
        int chamadas;

        @Override
        public Optional<PreferenciaHorario> interpretar(String texto, LocalDateTime agora, ZoneId fuso) {
            chamadas++;
            if (falhar) {
                throw new FalhaIntegracaoException("IA indisponível");
            }
            return resposta;
        }
    }

    static final class EnviadorFake implements EnviadorWhatsApp {
        final List<String> textos = new ArrayList<>();
        final List<String> templates = new ArrayList<>();
        boolean falhar;

        @Override
        public String enviarTexto(WaId destino, String texto) {
            if (falhar) {
                throw new FalhaIntegracaoException("Meta fora do ar");
            }
            textos.add(texto);
            return "wamid.enviada." + textos.size();
        }

        @Override
        public String enviarTemplate(WaId destino, String nomeTemplate, String idioma, List<String> parametros) {
            templates.add(nomeTemplate + parametros);
            return "wamid.template." + templates.size();
        }

        String ultimo() {
            return textos.getLast();
        }
    }

    static final class CanalFake implements CanalNotificacaoConsultorio {
        final String nome;
        final List<AvisoAgendamento> avisos = new ArrayList<>();
        final List<AlertaConsultorio> alertas = new ArrayList<>();
        boolean habilitado = true;
        boolean falhar;

        CanalFake(String nome) {
            this.nome = nome;
        }

        @Override
        public String nome() {
            return nome;
        }

        @Override
        public boolean habilitado() {
            return habilitado;
        }

        @Override
        public void notificarAgendamento(AvisoAgendamento aviso) {
            if (falhar) {
                throw new FalhaIntegracaoException("canal fora do ar");
            }
            avisos.add(aviso);
        }

        @Override
        public void notificarAlerta(AlertaConsultorio alerta) {
            if (falhar) {
                throw new FalhaIntegracaoException("canal fora do ar");
            }
            alertas.add(alerta);
        }
    }

    static final class PublicadorFake implements PublicadorEventos {
        final List<Object> eventos = new ArrayList<>();

        @Override
        public void publicar(Object evento) {
            eventos.add(evento);
        }

        <T> List<T> eventos(Class<T> tipo) {
            return eventos.stream().filter(tipo::isInstance).map(tipo::cast).toList();
        }
    }

    static final class MidiaFake implements ProvedorMidia {
        @Override
        public MidiaBaixada baixar(String midiaId) {
            return new MidiaBaixada(new byte[]{1, 2, 3}, "audio/ogg");
        }
    }

    static final class TranscritorFake implements TranscritorAudio {
        Optional<String> transcricao = Optional.empty();
        boolean disponivel;

        @Override
        public boolean disponivel() {
            return disponivel;
        }

        @Override
        public Optional<String> transcrever(MidiaBaixada audio) {
            return transcricao;
        }
    }
}
