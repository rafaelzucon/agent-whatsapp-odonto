package br.com.consultorio.odonto.adaptador.saida.plataforma;

import br.com.consultorio.odonto.aplicacao.porta.saida.PublicadorEventos;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Publica os eventos da aplicação no barramento de eventos do Spring. */
@Component
public class SpringPublicadorEventos implements PublicadorEventos {

    private final ApplicationEventPublisher publicador;

    public SpringPublicadorEventos(ApplicationEventPublisher publicador) {
        this.publicador = publicador;
    }

    @Override
    public void publicar(Object evento) {
        publicador.publishEvent(evento);
    }
}
