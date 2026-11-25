package io.github.igorpadua.imoveis.imovel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static io.github.igorpadua.imoveis.imovel.CaracteristicasBuilder.umaCaracteristica;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaracteristicasTest {

    @Test
    void aceitaZeroQuartosComZeroSuites() {
        assertThat(umaCaracteristica().comSuites(0).comQuartos(0).build().quartos()).isZero();
    }

    @Test
    void aceitaZeroSuites() {
        assertThat(umaCaracteristica().comSuites(0).build().suites()).isZero();
    }

    @Test
    void aceitaZeroBanheiros() {
        assertThat(umaCaracteristica().comBanheiros(0).build().banheiros()).isZero();
    }

    @Test
    void aceitaZeroVagas() {
        assertThat(umaCaracteristica().comVagas(0).build().vagas()).isZero();
    }

    @Test
    void recusaQuartosNegativos() {
        assertThatThrownBy(() -> umaCaracteristica().comQuartos(-1).build())
                .isInstanceOf(RegraDeDominioException.class)
                .hasMessageContaining("Quantidade de quartos não pode ser negativa");
    }

    @Test
    void recusaSuitesNegativas() {
        assertThatThrownBy(() -> umaCaracteristica().comSuites(-1).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void recusaBanheirosNegativos() {
        assertThatThrownBy(() -> umaCaracteristica().comBanheiros(-1).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void recusaVagasNegativas() {
        assertThatThrownBy(() -> umaCaracteristica().comVagas(-1).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void recusaSuitesAcimaDosQuartos() {
        assertThatThrownBy(() -> umaCaracteristica().comQuartos(1).comSuites(2).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void aceitaSuitesIguaisAosQuartos() {
        assertThat(umaCaracteristica().comSuites(1).comQuartos(1).build().suites()).isEqualTo(1);
    }

    @Test
    void aceitaSuitesAbaixoDosQuartos() {
        assertThat(umaCaracteristica().comSuites(1).comQuartos(2).build().suites()).isEqualTo(1);
    }

    @Test
    void aceitaAreaUtilNula() {
        assertThat(umaCaracteristica().comAreaUtil(null).build().areaUtil()).isNull();
    }

    @ParameterizedTest(name = "Área útil {0} (zero) é recusada")
    @CsvSource({"0", "0.0"})
    void recusaAreaUtilZero(String areaUtil) {
        assertThatThrownBy(() -> umaCaracteristica().comAreaUtil(areaUtil).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "Área útil {0} (negativa) é recusada")
    @CsvSource({"-0.01", "-50.0"})
    void recusaAreaUtilNegativa(String areaUtil) {
        assertThatThrownBy(() -> umaCaracteristica().comAreaUtil(areaUtil).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void aceitaAMenorAreaUtilPossivel() {
        assertThat(umaCaracteristica().comAreaUtil("0.01").build().areaUtil()).isEqualTo(new BigDecimal("0.01"));
    }

    @Test
    void aceitaAMaiorAreaUtilQueCabeNaColuna() {
        assertThat(umaCaracteristica().comAreaUtil("99999999.99").build().areaUtil()).isEqualTo(new BigDecimal("99999999.99"));
    }

    @Test
    void recusaAreaUtilAcimaDoLimiteDaColuna() {
        assertThatThrownBy(() -> umaCaracteristica().comAreaUtil("100000000.0").build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "Área útil {0} com mais de duas casas significativas é recusada")
    @CsvSource({"72.501", "0.001"})
    void recusaAreaUtilComMaisDeDuasCasas(String areaUtil) {
        assertThatThrownBy(() -> umaCaracteristica().comAreaUtil(areaUtil).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "Área útil {0} tem os zeros à direita ignorados e é guardada como {1}")
    @CsvSource({"72.500, 72.50", "100.000, 100.00"})
    void aceitaAreaUtilComZerosADireitaAlemDaSegundaCasa(String areaUtil, String areaUtilEsperada) {
        assertThat(umaCaracteristica().comAreaUtil(areaUtil).build().areaUtil()).isEqualTo(new BigDecimal(areaUtilEsperada));
    }

    @ParameterizedTest(name = "Área útil {0} é guardada com escala 2 como {1}")
    @CsvSource({"72, 72.00", "72.5, 72.50"})
    void guardaAreaUtilSempreComEscalaDois(String areaUtil, String areaUtilEsperada) {
        assertThat(umaCaracteristica().comAreaUtil(areaUtil).build().areaUtil()).isEqualTo(new BigDecimal(areaUtilEsperada));
    }

    @Test
    void recusaSuiteSemQuarto() {
        assertThatThrownBy(() -> umaCaracteristica().comSuites(1).comQuartos(0).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

}
