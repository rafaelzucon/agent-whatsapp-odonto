package br.com.consultorio.odonto.aplicacao.porta.entrada;

/** Retenção (LGPD): apaga mensagens antigas e conversas encerradas antigas. Nunca apaga prontuários. */
public interface ExpurgarMensagensUseCase {

    ResultadoExpurgo expurgar();

    record ResultadoExpurgo(int mensagens, int conversas) {
    }
}
