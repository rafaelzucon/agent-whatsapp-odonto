package br.com.consultorio.odonto.adaptador.entrada.web;

import br.com.consultorio.odonto.aplicacao.porta.entrada.ConsultarCatalogoUseCase;
import br.com.consultorio.odonto.aplicacao.porta.entrada.ProblemaVisao;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Catálogo de problemas odontológicos usado na classificação das queixas. */
@RestController
@RequestMapping("/api/admin/catalogo")
public class CatalogoController {

    private final ConsultarCatalogoUseCase catalogo;

    public CatalogoController(ConsultarCatalogoUseCase catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping
    public List<ProblemaVisao> listar() {
        return catalogo.listar();
    }
}
