package br.com.consultorio.odonto.dominio.excecao;

/**
 * O horário pedido não está disponível na agenda.
 */
public class HorarioIndisponivelException extends DominioException {

    private final MotivoIndisponibilidade motivo;

    public HorarioIndisponivelException(MotivoIndisponibilidade motivo) {
        super("Horário indisponível: " + motivo.descricao());
        this.motivo = motivo;
    }

    public MotivoIndisponibilidade motivo() {
        return motivo;
    }
}
