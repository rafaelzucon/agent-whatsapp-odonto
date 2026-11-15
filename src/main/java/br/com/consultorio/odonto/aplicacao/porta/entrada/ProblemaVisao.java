package br.com.consultorio.odonto.aplicacao.porta.entrada;

import java.util.List;
import java.util.Optional;

public record ProblemaVisao(String codigo, String nome, String descricao, String especialidade, String prioridade,
                            Optional<String> cid10, List<String> palavrasChave) {
}
