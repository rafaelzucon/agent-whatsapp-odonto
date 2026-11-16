package br.com.consultorio.odonto.adaptador.saida.persistencia;

import br.com.consultorio.odonto.adaptador.saida.persistencia.entidade.BloqueioAgendaEntity;
import br.com.consultorio.odonto.adaptador.saida.persistencia.jpa.BloqueioAgendaJpaRepository;
import br.com.consultorio.odonto.aplicacao.porta.saida.BloqueioAgendaRepositorio;
import br.com.consultorio.odonto.dominio.modelo.BloqueioAgenda;
import br.com.consultorio.odonto.dominio.modelo.Horario;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class BloqueioAgendaJpaAdapter implements BloqueioAgendaRepositorio {

    private final BloqueioAgendaJpaRepository repositorio;
    private final FusoPersistencia fuso;

    BloqueioAgendaJpaAdapter(BloqueioAgendaJpaRepository repositorio, FusoPersistencia fuso) {
        this.repositorio = repositorio;
        this.fuso = fuso;
    }

    @Override
    @Transactional
    public BloqueioAgenda salvar(BloqueioAgenda bloqueio) {
        return paraDominio(repositorio.save(new BloqueioAgendaEntity(bloqueio.id().orElse(null),
                fuso.paraInstante(bloqueio.periodo().inicio()), fuso.paraInstante(bloqueio.periodo().fim()),
                bloqueio.motivo())));
    }

    @Override
    @Transactional
    public boolean remover(long id) {
        if (!repositorio.existsById(id)) {
            return false;
        }
        repositorio.deleteById(id);
        return true;
    }

    @Override
    public List<BloqueioAgenda> listarEntre(LocalDateTime inicio, LocalDateTime fim) {
        return repositorio.findByInicioLessThanAndFimGreaterThanOrderByInicio(fuso.paraInstante(fim),
                fuso.paraInstante(inicio)).stream().map(this::paraDominio).toList();
    }

    private BloqueioAgenda paraDominio(BloqueioAgendaEntity e) {
        return new BloqueioAgenda(Optional.of(e.getId()),
                new Horario(fuso.paraLocal(e.getInicio()), fuso.paraLocal(e.getFim())), e.getMotivo());
    }
}
