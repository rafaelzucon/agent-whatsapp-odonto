package br.com.consultorio.odonto.dominio.modelo;

import br.com.consultorio.odonto.dominio.servico.ValidadorDados;

import java.util.Optional;

import static br.com.consultorio.odonto.dominio.modelo.ContextoEtapa.exigir;

/**
 * {@link EtapaConversa#AGUARDANDO_CONSENTIMENTO}: com "SIM" pede o cadastro; com "NÃO" descarta os dados
 * pessoais coletados (LGPD) e avisa o consultório.
 */
final class TratadorConsentimento implements TratadorEtapa {

    private final ValidadorDados validador;

    TratadorConsentimento(ValidadorDados validador) {
        this.validador = validador;
    }

    @Override
    public AcaoConversa tratar(String resposta, ContextoEtapa contexto) {
        Optional<Boolean> concorda = validador.interpretarSimNao(resposta);
        if (concorda.isEmpty()) {
            return contexto.respostaInvalida();
        }
        DadosConversa dados = contexto.dados();
        if (concorda.get()) {
            return AcaoConversa.executar(new ComandoConversa.CadastrarPaciente(
                    exigir(dados.rg(), "RG"), exigir(dados.nome(), "nome"),
                    exigir(dados.dataNascimento(), "data de nascimento"), dados.responsavel()));
        }
        contexto.atualizar(DadosConversa.vazio());
        contexto.avancarPara(EtapaConversa.ENCERRADA_SEM_CONSENTIMENTO);
        return AcaoConversa.responder(RespostaAgente.de(TipoResposta.SEM_CONSENTIMENTO))
                .comAlerta(contexto.alerta(TipoAlerta.CONSENTIMENTO_RECUSADO,
                        "Paciente novo recusou o consentimento para cadastro pelo WhatsApp e aguarda contato."));
    }
}
