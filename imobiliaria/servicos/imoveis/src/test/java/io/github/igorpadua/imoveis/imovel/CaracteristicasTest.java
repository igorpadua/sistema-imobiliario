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
    void aceitaQuartoZerado() {
        assertThat(umaCaracteristica().comSuites(0).comQuartos(0).build().quartos()).isZero();
    }

    @Test
    void aceitaSuiteZerada() {
        assertThat(umaCaracteristica().comSuites(0).build().suites()).isZero();
    }

    @Test
    void aceitaBanheiroZerado() {
        assertThat(umaCaracteristica().comBanheiros(0).build().banheiros()).isZero();
    }

    @Test
    void aceitaVagasZerado() {
        assertThat(umaCaracteristica().comVagas(0).build().vagas()).isZero();
    }

    @Test
    void recusaQuartosNegativo() {
        assertThatThrownBy(() -> umaCaracteristica().comQuartos(-1).build())
                .isInstanceOf(RegraDeDominioException.class)
                .hasMessageContaining("não pode ser negativo.");
    }

    @Test
    void recusaSuiteNegativa() {
        assertThatThrownBy(() -> umaCaracteristica().comSuites(-1).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void recusaBanheiroNegativo() {
        assertThatThrownBy(() -> umaCaracteristica().comBanheiros(-1).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void recusaVagasNegativo() {
        assertThatThrownBy(() -> umaCaracteristica().comVagas(-1).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void recusaSuiteMaiorQueQuarto() {
        assertThatThrownBy(() -> umaCaracteristica().comQuartos(1).comSuites(2).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void aceitaSuitesIguaisAoQuartos() {
        assertThat(umaCaracteristica().comSuites(1).comQuartos(1).build().suites()).isEqualTo(1);
    }

    @Test
    void aceitaSuitesAbaixoDoQuarto() {
        assertThat(umaCaracteristica().comSuites(1).comQuartos(2).build().suites()).isEqualTo(1);
    }

    @Test
    void aceitaAreaNula() {
        assertThat(umaCaracteristica().comAreaUtil(null).build().areaUtil()).isNull();
    }

    @ParameterizedTest(name = "Área {0} zero é recusado")
    @CsvSource({"0", "0.0"})
    void recusaAreaZero(String area) {
        assertThatThrownBy(() -> umaCaracteristica().comAreaUtil(area).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "Área {0} negativa é recusado")
    @CsvSource({"-0.01", "-50.0"})
    void recusaAreaNegativa(String area) {
        assertThatThrownBy(() -> umaCaracteristica().comAreaUtil(area).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @Test
    void aceitaAMenorAreaPossivel() {
        assertThat(umaCaracteristica().comAreaUtil("0.01").build().areaUtil()).isEqualTo(new BigDecimal("0.01"));
    }

    @Test
    void aceitaAMaiorAreaPossivel() {
        assertThat(umaCaracteristica().comAreaUtil("99999999.99").build().areaUtil()).isEqualTo(new BigDecimal("99999999.99"));
    }

    @Test
    void recusaAreaAcimaLimite() {
        assertThatThrownBy(() -> umaCaracteristica().comAreaUtil("100000000.0").build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "Área {0} recusa mais que duas casas decimais significativas")
    @CsvSource({"72.501", "0.001"})
    void recusaMaisQueDuasCasasDecimaisSignificativas(String areaUtil) {
        assertThatThrownBy(() -> umaCaracteristica().comAreaUtil(areaUtil).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

    @ParameterizedTest(name = "Área aceita zeros a direita recebe {0} o esperado é {1}")
    @CsvSource({"72.500, 72.50", "100.000, 100.00"})
    void aceitaZerosADireita(String area, String esperado) {
        assertThat(umaCaracteristica().comAreaUtil(area).build().areaUtil()).isEqualTo(new BigDecimal(esperado));
    }

    @ParameterizedTest(name = "Área sempre {0} sempre guarda duas casas decimais")
    @CsvSource({"72, 72.00", "72.5, 72.50"})
    void sempreGuardaDuasCasasDecimais(String area, String esperado) {
        assertThat(umaCaracteristica().comAreaUtil(area).build().areaUtil()).isEqualTo(new BigDecimal(esperado));
    }

    @Test
    void recusaSuiteSemQuarto() {
        assertThatThrownBy(() -> umaCaracteristica().comSuites(1).comQuartos(0).build())
                .isInstanceOf(RegraDeDominioException.class);
    }

}
