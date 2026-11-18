package br.com.consultorio.odonto.adaptador.entrada.lambda;

/** Resposta HTTP mínima (corpo em texto puro). */
public record RespostaHttp(int status, String corpo) {

    public static RespostaHttp ok(String corpo) {
        return new RespostaHttp(200, corpo);
    }
}
