package br.com.consultorio.odonto.adaptador.saida.persistencia;

import br.com.consultorio.odonto.adaptador.saida.persistencia.entidade.PacienteEntity;
import br.com.consultorio.odonto.adaptador.saida.persistencia.jpa.PacienteJpaRepository;
import br.com.consultorio.odonto.aplicacao.porta.saida.CifradorDocumento;
import br.com.consultorio.odonto.aplicacao.porta.saida.PacienteRepositorio;
import br.com.consultorio.odonto.dominio.modelo.DocumentoRg;
import br.com.consultorio.odonto.dominio.modelo.Paciente;
import br.com.consultorio.odonto.dominio.modelo.WaId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Pacientes com dados pessoais cifrados; o RG é localizado pelo HMAC. */
@Repository
@PersistenciaJpa
@Transactional(readOnly = true)
public class PacienteJpaAdapter implements PacienteRepositorio {

    private static final String SEPARADOR_RG = "|";

    private final PacienteJpaRepository repositorio;
    private final CifradorDocumento cifrador;

    public PacienteJpaAdapter(PacienteJpaRepository repositorio, CifradorDocumento cifrador) {
        this.repositorio = repositorio;
        this.cifrador = cifrador;
    }

    @Override
    @Transactional
    public Paciente salvar(Paciente paciente) {
        PacienteEntity entidade = new PacienteEntity(paciente.id().orElse(null), cifrador.hash(paciente.rg()),
                paciente.rg().chaveCanonica(), paciente.nomeCompleto(), paciente.dataNascimento().toString(),
                paciente.responsavelLegal().orElse(null),
                paciente.telefones().stream().map(WaId::valor).collect(Collectors.toCollection(java.util.LinkedHashSet::new)),
                paciente.consentimentoLgpd(), paciente.dataConsentimento().orElse(null), paciente.dataCadastro());
        return paraDominio(repositorio.save(entidade));
    }

    @Override
    public Optional<Paciente> buscarPorId(long id) {
        return repositorio.findById(id).map(PacienteJpaAdapter::paraDominio);
    }

    @Override
    public Optional<Paciente> buscarPorRgHash(String rgHash) {
        return repositorio.findByRgHash(rgHash).map(PacienteJpaAdapter::paraDominio);
    }

    @Override
    public List<Paciente> listarTodos() {
        return repositorio.findAll().stream().map(PacienteJpaAdapter::paraDominio).toList();
    }

    static Paciente paraDominio(PacienteEntity e) {
        int separador = e.getRg().lastIndexOf(SEPARADOR_RG);
        DocumentoRg rg = DocumentoRg.de(e.getRg().substring(0, separador), e.getRg().substring(separador + 1));
        return Paciente.restaurar(e.getId(), rg, e.getNome(), LocalDate.parse(e.getDataNascimento()),
                e.getResponsavel(), e.getTelefones().stream().map(WaId::new).collect(Collectors.toSet()),
                e.isConsentimentoLgpd(), e.getDataConsentimento(), e.getDataCadastro());
    }
}
