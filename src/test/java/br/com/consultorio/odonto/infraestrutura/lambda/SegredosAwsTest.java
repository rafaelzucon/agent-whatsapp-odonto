package br.com.consultorio.odonto.infraestrutura.lambda;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ssm.model.Parameter;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SegredosAwsTest {

    @Test
    void usaOUltimoSegmentoDoNomeDoParametroComoNomeDaVariavel() {
        assertThat(SegredosAws.nomeCurto(Parameter.builder().name("/odonto/segredos/WHATSAPP_TOKEN_ACESSO").build()))
                .isEqualTo("WHATSAPP_TOKEN_ACESSO");
    }

    @Test
    void resolvedorPrefereOSegredoEDepoisOAmbiente() {
        var resolvedor = SegredosAws.resolvedor(Map.of("PATH", "do-segredo"));

        assertThat(resolvedor.apply("PATH")).isEqualTo("do-segredo");
        assertThat(resolvedor.apply("ODONTO_VARIAVEL_QUE_NAO_EXISTE")).isNull();
    }

    @Test
    void publicaComoPropriedadeDeSistemaSemSobrescrever() {
        System.setProperty("ODONTO_TESTE_JA_DEFINIDA", "original");
        try {
            SegredosAws.publicarComoPropriedadesDoSistema(Map.of("ODONTO_TESTE_JA_DEFINIDA", "novo",
                    "ODONTO_TESTE_NOVA", "valor"));

            assertThat(System.getProperty("ODONTO_TESTE_JA_DEFINIDA")).isEqualTo("original");
            assertThat(System.getProperty("ODONTO_TESTE_NOVA")).isEqualTo("valor");
        } finally {
            System.clearProperty("ODONTO_TESTE_JA_DEFINIDA");
            System.clearProperty("ODONTO_TESTE_NOVA");
        }
    }
}
