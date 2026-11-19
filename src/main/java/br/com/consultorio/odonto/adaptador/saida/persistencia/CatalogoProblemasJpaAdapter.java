package br.com.consultorio.odonto.adaptador.saida.persistencia;

import br.com.consultorio.odonto.adaptador.saida.persistencia.entidade.ProblemaOdontologicoEntity;
import br.com.consultorio.odonto.adaptador.saida.persistencia.jpa.ProblemaOdontologicoJpaRepository;
import br.com.consultorio.odonto.aplicacao.porta.saida.CatalogoProblemasRepositorio;
import br.com.consultorio.odonto.dominio.modelo.ProblemaOdontologico;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Catálogo de problemas odontológicos (somente leitura; mantido por migrações SQL). */
@Repository
@PersistenciaJpa
@Transactional(readOnly = true)
public class CatalogoProblemasJpaAdapter implements CatalogoProblemasRepositorio {

    private final ProblemaOdontologicoJpaRepository repositorio;

    public CatalogoProblemasJpaAdapter(ProblemaOdontologicoJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public List<ProblemaOdontologico> listarAtivos() {
        return repositorio.findByAtivoTrueOrderByCodigo().stream().map(CatalogoProblemasJpaAdapter::paraDominio).toList();
    }

    /** Busca também itens desativados, para que queixas antigas continuem legíveis. */
    @Override
    public Optional<ProblemaOdontologico> buscarPorCodigo(String codigo) {
        return repositorio.findById(codigo).map(CatalogoProblemasJpaAdapter::paraDominio);
    }

    static ProblemaOdontologico paraDominio(ProblemaOdontologicoEntity entidade) {
        List<String> palavras = entidade.getPalavrasChave() == null ? List.of()
                : Arrays.stream(entidade.getPalavrasChave().split(";")).map(String::trim).filter(p -> !p.isEmpty())
                .toList();
        return new ProblemaOdontologico(entidade.getCodigo(), entidade.getNome(), entidade.getDescricao(),
                entidade.getEspecialidade(), entidade.getPrioridade(), Optional.ofNullable(entidade.getCid10()),
                palavras);
    }
}
