package io.github.igorpadua.imoveis.imovel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ProprietariosTest {

    private static final UUID PESSOA_A = UUID.randomUUID();

    @Test
    void recusaListaVazia() {
        assertThatThrownBy(() -> proprietariosComPercentual(List.of()))
                .isInstanceOf(RegraDeDominioException.class)
                .hasMessageContaining("A lista de proprietários deve ter ao menos um proprietário.");
    }

    @Test
    void recusaListaNula() {
        assertThatThrownBy(() -> new Proprietarios(null))
                .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void recusaListaComItemNulo() {
        List<Proprietario> comNulo = Arrays.asList(
                new Proprietario(UUID.randomUUID(), new BigDecimal("100")),
                null
        );

        assertThatThrownBy(() -> new Proprietarios(comNulo))
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "Percentuais {0} somam 100 e são aceitos")
    @MethodSource("percentuaisQueSomamCem")
    void aceitaPercentuaisQueSomamCem(List<String> percentuais) {
        List<Proprietario> listaProprietarios = listaComPercentuais(percentuais);

        Proprietarios proprietarios = new Proprietarios(listaProprietarios);

        assertThat(proprietarios.proprietarios()).containsExactlyElementsOf(listaProprietarios);
    }

    static Stream<List<String>> percentuaisQueSomamCem() {
        return Stream.of(
                List.of("100"),
                List.of("50", "50"),
                List.of("33.33", "33.33", "33.34"),
                List.of("33.3333", "33.3333", "33.3334")
        );
    }

    @ParameterizedTest(name = "Percentuais {0} somam {1} e são recusados")
    @MethodSource("percentuaisQueNaoSomamCem")
    void recusaPercentuaisQueNaoSomamCem(List<String> percentuais, String somaInformada) {
        assertThatThrownBy(() -> proprietariosComPercentual(percentuais))
                .isInstanceOf(RegraDeDominioException.class)
                .hasMessageContaining("(informada: " + somaInformada + ")");
    }

    static Stream<Arguments> percentuaisQueNaoSomamCem() {
        return Stream.of(
                arguments(List.of("33.33", "33.33", "33.33"), "99.9900"),
                arguments(List.of("33.3333", "33.3333", "33.3333"), "99.9999"),
                arguments(List.of("50.0001", "50"), "100.0001"),
                arguments(List.of("60", "50"), "110.0000")
        );
    }

    @Test
    void recusaPessoaIdRepetido() {
        List<Proprietario> proprietariosRepetidos = Arrays.asList(
                new Proprietario(PESSOA_A, new BigDecimal("50")),
                new Proprietario(PESSOA_A, new BigDecimal("50"))
        );

        assertThatThrownBy(() -> new Proprietarios(proprietariosRepetidos))
                .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void recusaPessoaIdRepetidoForaDeSequencia() {
        List<Proprietario> proprietariosRepetidos = Arrays.asList(
                new Proprietario(PESSOA_A, new BigDecimal("30")),
                new Proprietario(UUID.randomUUID(), new BigDecimal("40")),
                new Proprietario(PESSOA_A, new BigDecimal("30"))
        );

        assertThatThrownBy(() -> new Proprietarios(proprietariosRepetidos))
                .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void alterarAListaOriginalDepoisDaCriacaoNaoAlteraOValueObject() {
        List<Proprietario> listaProprietarios = new ArrayList<>(List.of(
                new Proprietario(PESSOA_A, new BigDecimal("100"))
        ));
        Proprietarios proprietarios = new Proprietarios(listaProprietarios);
        listaProprietarios.clear();

        assertThat(proprietarios.proprietarios()).containsExactly(new Proprietario(PESSOA_A, paraBigDecimal("100")));
    }

    @Test
    void listaDevolvidaNaoAceitaAlteracao() {
        List<Proprietario> listaProprietarios = new ArrayList<>(List.of(
                new Proprietario(PESSOA_A, new BigDecimal("100"))
        ));
        Proprietarios proprietarios = new Proprietarios(listaProprietarios);
        assertThatThrownBy(() -> proprietarios.proprietarios().add(new Proprietario(UUID.randomUUID(), paraBigDecimal("50"))))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aceitaListaImutavelNaEntrada() {
        Proprietario proprietario = new Proprietario(PESSOA_A, paraBigDecimal("100"));
        Proprietarios proprietarios = new Proprietarios(List.of(proprietario));

        assertThat(proprietarios.proprietarios()).containsExactly(proprietario);
    }

    private static Proprietarios proprietariosComPercentual(List<String> percentuais) {
        return new Proprietarios(listaComPercentuais(percentuais));
    }

    private static List<Proprietario> listaComPercentuais(List<String> percentuais) {
        return percentuais.stream()
                .map(percentual -> new Proprietario(UUID.randomUUID(), paraBigDecimal(percentual)))
                .toList();
    }

    private static BigDecimal paraBigDecimal(String valor) {
        return valor == null ? null : new BigDecimal(valor);
    }
}
