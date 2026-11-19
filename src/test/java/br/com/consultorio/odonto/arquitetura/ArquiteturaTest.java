package br.com.consultorio.odonto.arquitetura;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Regras da Clean Architecture verificadas a cada build: se alguém quebrar a regra de dependência,
 * o teste falha e aponta a classe culpada.
 */
@AnalyzeClasses(packages = "br.com.consultorio.odonto", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    private static final String BASE = "br.com.consultorio.odonto";
    private static final String DOMINIO = BASE + ".dominio..";
    private static final String APLICACAO = BASE + ".aplicacao..";
    private static final String ADAPTADOR = BASE + ".adaptador..";
    private static final String INFRAESTRUTURA = BASE + ".infraestrutura..";

    @ArchTest
    static final ArchRule dominio_usa_apenas_java_puro = classes().that().resideInAPackage(DOMINIO)
            .should().onlyDependOnClassesThat().resideInAnyPackage(DOMINIO, "java..")
            .because("o domínio é o núcleo: regras de negócio sem framework nem biblioteca externa");

    @ArchTest
    static final ArchRule camadas_respeitam_a_regra_de_dependencia = layeredArchitecture().consideringAllDependencies()
            .layer("Domínio").definedBy(DOMINIO)
            .layer("Aplicação").definedBy(APLICACAO)
            .layer("Adaptador").definedBy(ADAPTADOR)
            .layer("Infraestrutura").definedBy(INFRAESTRUTURA)
            .whereLayer("Infraestrutura").mayNotBeAccessedByAnyLayer()
            .whereLayer("Adaptador").mayOnlyBeAccessedByLayers("Infraestrutura")
            .whereLayer("Aplicação").mayOnlyBeAccessedByLayers("Adaptador", "Infraestrutura")
            .whereLayer("Domínio").mayOnlyBeAccessedByLayers("Aplicação", "Adaptador", "Infraestrutura")
            .because("o código só pode depender de camadas mais internas");

    @ArchTest
    static final ArchRule aplicacao_nao_conhece_frameworks = classes().that().resideInAPackage(APLICACAO)
            .should().onlyDependOnClassesThat().resideInAnyPackage(APLICACAO, DOMINIO, "java..")
            .because("casos de uso e portas não dependem de Spring, JPA, Jackson nem Spring AI");

    @ArchTest
    static final ArchRule adaptadores_de_entrada_e_saida_sao_independentes = noClasses()
            .that().resideInAPackage(BASE + ".adaptador.entrada..")
            .should().dependOnClassesThat().resideInAPackage(BASE + ".adaptador.saida..")
            .andShould().dependOnClassesThat().resideInAPackage(BASE + ".aplicacao.casodeuso..")
            .because("a entrada conversa com a aplicação só pelas portas de entrada");

    @ArchTest
    static final ArchRule spring_ai_fica_nos_adaptadores_de_ia = noClasses()
            .that().resideOutsideOfPackages(BASE + ".adaptador.saida.ia..", BASE + ".adaptador.saida.transcricao..",
                    INFRAESTRUTURA)
            .should().dependOnClassesThat().resideInAPackage("org.springframework.ai..")
            .because("a IA é um detalhe de infraestrutura atrás das portas ClassificadorProblemaIA, "
                    + "InterpretadorDataHoraIA e TranscritorAudio");

    @ArchTest
    static final ArchRule jpa_fica_no_adaptador_de_persistencia = noClasses()
            .that().resideOutsideOfPackages(BASE + ".adaptador.saida.persistencia..", INFRAESTRUTURA)
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..",
                    "org.springframework.data..")
            .because("o mecanismo de persistência fica escondido atrás dos repositórios (portas de saída)");

    @ArchTest
    static final ArchRule sem_ciclos_entre_modulos = slices().matching(BASE + ".(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule sem_ciclos_na_aplicacao = slices().matching(BASE + ".aplicacao.(*)..").should()
            .beFreeOfCycles();

    @ArchTest
    static final ArchRule sem_ciclos_entre_adaptadores = slices().matching(BASE + ".adaptador.(*).(*)..").should()
            .beFreeOfCycles();

    @ArchTest
    static final ArchRule controladores_usam_apenas_portas_de_entrada = noClasses()
            .that().resideInAnyPackage(BASE + ".adaptador.entrada.web..", BASE + ".adaptador.entrada.webhook..")
            .should().dependOnClassesThat().resideInAnyPackage(BASE + ".aplicacao.porta.saida..",
                    BASE + ".aplicacao.casodeuso..", BASE + ".adaptador.saida..")
            .because("controladores só traduzem HTTP para chamadas de casos de uso pelas portas de entrada");

    @ArchTest
    static final ArchRule aws_fica_na_infraestrutura_do_lambda = noClasses()
            .that().resideOutsideOfPackages(BASE + ".infraestrutura.lambda..",
                    BASE + ".adaptador.saida.persistencia.dynamodb..")
            .should().dependOnClassesThat().resideInAnyPackage("com.amazonaws..", "software.amazon..", "org.crac..")
            .because("o AWS Lambda é só uma forma de implantação e o DynamoDB só mais um adaptador de persistência: "
                    + "os adaptadores do modo serverless (adaptador.entrada.lambda) não conhecem a AWS e o resto do "
                    + "sistema nem sabe que ela existe");

    @ArchTest
    static final ArchRule adaptador_lambda_usa_apenas_portas_de_entrada = noClasses()
            .that().resideInAPackage(BASE + ".adaptador.entrada.lambda..")
            .should().dependOnClassesThat().resideInAnyPackage(BASE + ".aplicacao.porta.saida..",
                    BASE + ".aplicacao.casodeuso..", BASE + ".adaptador.saida..")
            .because("assim como os controladores, o adaptador serverless só traduz eventos em casos de uso");

    /** Classe concreta do sistema que implementa uma abstração do próprio sistema (porta, estratégia, canal...). */
    private static final DescribedPredicate<JavaClass> IMPLEMENTACAO_DE_ABSTRACAO_DO_SISTEMA =
            new DescribedPredicate<>("uma classe concreta que implementa uma interface do sistema") {
                @Override
                public boolean test(JavaClass tipo) {
                    return !tipo.isInterface() && !tipo.isRecord() && !tipo.isEnum()
                            && !tipo.getModifiers().contains(JavaModifier.ABSTRACT)
                            && tipo.getAllRawInterfaces().stream().anyMatch(i -> i.getPackageName().startsWith(BASE));
                }
            };

    @ArchTest
    static final ArchRule dependencias_apontam_para_abstracoes = fields()
            .that().areDeclaredInClassesThat().resideInAnyPackage(APLICACAO, ADAPTADOR)
            .and().areNotStatic()
            .should().notHaveRawType(IMPLEMENTACAO_DE_ABSTRACAO_DO_SISTEMA)
            .because("inversão de dependência (DIP): casos de uso e adaptadores guardam a interface (porta, "
                    + "estratégia, canal), nunca a implementação; quem escolhe a implementação é a composição");

    @ArchTest
    static final ArchRule sem_injecao_por_campo = NO_CLASSES_SHOULD_USE_FIELD_INJECTION
            .because("dependências entram pelo construtor, o que deixa as classes testáveis e imutáveis");
}
