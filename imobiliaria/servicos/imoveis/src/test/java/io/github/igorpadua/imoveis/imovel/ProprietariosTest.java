package io.github.igorpadua.imoveis.imovel;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void aceitaUmUnicoProprietarioComCemPorCento() {
        List<Proprietario> listaProprietario = List.of(new Proprietario(PESSOA_A, paraBigDecimal("100")));
        Proprietarios proprietarios = new Proprietarios(listaProprietario);
        assertThat(proprietarios.proprietarios()).containsExactlyElementsOf(listaProprietario);
    }

    @Test
    void aceitaSomaExataComQuatroCasas() {
        List<Proprietario> listaProprietario = List.of(
                new Proprietario(UUID.randomUUID(), paraBigDecimal("33.3333")),
                new Proprietario(UUID.randomUUID(), paraBigDecimal("33.3333")),
                new Proprietario(UUID.randomUUID(), paraBigDecimal("33.3334"))
        );
        Proprietarios proprietarios = new Proprietarios(listaProprietario);
        assertThat(proprietarios.proprietarios()).containsExactlyElementsOf(listaProprietario);
    }

    @Test
    void recusaSomaAbaixoDeCem() {
        assertThatThrownBy(() -> proprietariosComPercentual(List.of("33.3333", "33.3333", "33.3333")))
                .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void recusaSomaAcimaDeCem() {
        assertThatThrownBy(() -> proprietariosComPercentual(List.of("50.0001", "50")))
                .isInstanceOf(RegraDeDominioException.class);
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
        return new Proprietarios(
                percentuais.stream().map( percentual ->
                        new Proprietario(UUID.randomUUID(), paraBigDecimal(percentual))
                ).toList()
        );
    }

    private static BigDecimal paraBigDecimal(String valor) {
        return valor == null ? null : new BigDecimal(valor);
    }
}
