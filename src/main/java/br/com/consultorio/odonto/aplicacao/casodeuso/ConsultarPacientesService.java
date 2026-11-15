package br.com.consultorio.odonto.aplicacao.casodeuso;

import br.com.consultorio.odonto.aplicacao.excecao.RecursoNaoEncontradoException;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ConsultarPacientesUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.PacienteVisao;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ProntuarioVisao;
import br.com.consultorio.odonto.aplicacao.porta.saida.CifradorDocumento;
import br.com.consultorio.odonto.aplicacao.porta.saida.ConsultaRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.PacienteRepositorio;
import br.com.consultorio.odonto.aplicacao.porta.saida.ProntuarioRepositorio;
import br.com.consultorio.odonto.dominio.excecao.DadoInvalidoException;
import br.com.consultorio.odonto.dominio.modelo.DocumentoRg;
import br.com.consultorio.odonto.dominio.modelo.Paciente;
import br.com.consultorio.odonto.dominio.modelo.Prontuario;
import br.com.consultorio.odonto.dominio.servico.NormalizadorTexto;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Consultas da recepção sobre pacientes e prontuários. Como o nome é gravado cifrado, a busca por nome é feita
 * em memória sobre os pacientes decifrados (adequado ao porte de um consultório).
 */
public final class ConsultarPacientesService implements ConsultarPacientesUseCase {

    static final int TAMANHO_MINIMO_BUSCA = 3;
    static final int LIMITE_RESULTADOS = 50;

    private final PacienteRepositorio pacientes;
    private final ProntuarioRepositorio prontuarios;
    private final ConsultaRepositorio consultas;
    private final CifradorDocumento cifrador;
    private final MontadorVisoes visoes;

    public ConsultarPacientesService(PacienteRepositorio pacientes, ProntuarioRepositorio prontuarios,
                                     ConsultaRepositorio consultas, CifradorDocumento cifrador, MontadorVisoes visoes) {
        this.pacientes = Objects.requireNonNull(pacientes);
        this.prontuarios = Objects.requireNonNull(prontuarios);
        this.consultas = Objects.requireNonNull(consultas);
        this.cifrador = Objects.requireNonNull(cifrador);
        this.visoes = Objects.requireNonNull(visoes);
    }

    @Override
    public List<PacienteVisao> buscarPorNome(String nome) {
        String termo = NormalizadorTexto.normalizar(nome == null ? "" : nome);
        if (termo.length() < TAMANHO_MINIMO_BUSCA) {
            throw new DadoInvalidoException("Informe ao menos " + TAMANHO_MINIMO_BUSCA + " letras do nome.");
        }
        return pacientes.listarTodos().stream()
                .filter(p -> NormalizadorTexto.normalizar(p.nomeCompleto()).contains(termo))
                .sorted(Comparator.comparing(Paciente::nomeCompleto))
                .limit(LIMITE_RESULTADOS)
                .map(visoes::paciente)
                .toList();
    }

    @Override
    public Optional<PacienteVisao> buscarPorRg(String rg, String uf) {
        DocumentoRg documento = DocumentoRg.de(rg, uf);
        return pacientes.buscarPorRgHash(cifrador.hash(documento)).map(visoes::paciente);
    }

    @Override
    public ProntuarioVisao prontuario(long pacienteId) {
        Paciente paciente = pacientes.buscarPorId(pacienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente " + pacienteId + " não encontrado."));
        Prontuario prontuario = prontuarios.buscarPorPacienteId(pacienteId).orElseThrow(
                () -> new RecursoNaoEncontradoException("Prontuário do paciente " + pacienteId + " não encontrado."));
        return new ProntuarioVisao(prontuario.numero(), prontuario.dataAbertura(),
                visoes.paciente(paciente, Optional.of(prontuario)),
                prontuario.queixas().stream().map(MontadorVisoes::queixa).toList(),
                visoes.consultas(consultas.listarPorPaciente(pacienteId)));
    }
}
