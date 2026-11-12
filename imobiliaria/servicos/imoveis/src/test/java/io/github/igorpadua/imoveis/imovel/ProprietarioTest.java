package io.github.igorpadua.imoveis.imovel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProprietarioTest {

    @Test
    void recusaPessoaIdNulo() {
        assertThatThrownBy(() -> proprietario(null, "100"))
                .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void recusaPercentualNulo() {
        assertThatThrownBy(() -> proprietarioComPercentual(null))
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "Percentual {0} rejeita valor zero")
    @CsvSource({"0", "0.0000", "0.00"})
    void recusaPercentualZero(String percentual) {
        assertThatThrownBy(() -> proprietarioComPercentual(percentual))
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "Percentual {0} negativo é reijetado")
    @CsvSource({"-50", "-0.0001"})
    void recusaPercentualNegativo(String percentual) {
        assertThatThrownBy(() -> proprietarioComPercentual(percentual))
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "Percentual {0} é aceito")
    @CsvSource({"0.0001, 0.0001", "100.00, 100.0000", "1, 1.0000"})
    void aceitaPercentualPositivo(String percentual, String percentualEsperado) {
        assertThat(proprietarioComPercentual(percentual).percentual()).isEqualTo(percentualEsperado);
    }

    @ParameterizedTest(name = "Percentual {0} com mais de quatro casas é recusado")
    @CsvSource({"33.33331", "0.00001"})
    void recusaPercentualComMaisQuatroCasas(String percentual) {
        assertThatThrownBy(() -> proprietarioComPercentual(percentual))
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "Percentual {0} aceita zeros à direita além e removendo {1}")
    @CsvSource({"33.33330, 33.3333", "100.00000, 100.0000"})
    void aceitaPercentualZeroADireitaAlem(String percentual, String percentualEsperado) {
        assertThat(proprietarioComPercentual(percentual).percentual()).isEqualTo(percentualEsperado);
    }

    private static Proprietario proprietario(UUID pessoaId, String percentual) {
        return new Proprietario(pessoaId, paraBigDecimal(percentual));
    }

    private static Proprietario proprietarioComPercentual(String percentual) {
        return new Proprietario(UUID.randomUUID(), paraBigDecimal(percentual));
    }

    private static BigDecimal paraBigDecimal(String valor) {
        return valor == null ? null : new BigDecimal(valor);
    }

}
