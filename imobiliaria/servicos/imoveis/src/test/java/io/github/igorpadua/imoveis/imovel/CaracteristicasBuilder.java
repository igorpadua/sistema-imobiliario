package io.github.igorpadua.imoveis.imovel;

import java.math.BigDecimal;

class CaracteristicasBuilder {

    private int quartos = 2;
    private int suites = 1;
    private int banheiros = 1;
    private int vagas = 1;
    private BigDecimal areaUtil = new BigDecimal("60.00");

    static CaracteristicasBuilder umaCaracteristica() {
        return new CaracteristicasBuilder();
    }

    CaracteristicasBuilder comQuartos(int quartos) {
        this.quartos = quartos;
        return this;
    }

    CaracteristicasBuilder comSuites(int suite) {
        this.suites = suite;
        return this;
    }

    CaracteristicasBuilder comBanheiros(int banheiro) {
        this.banheiros = banheiro;
        return this;
    }

    CaracteristicasBuilder comVagas(int vagas) {
        this.vagas = vagas;
        return this;
    }

    CaracteristicasBuilder comAreaUtil(String areaUtil) {
        this.areaUtil = areaUtil != null ? new BigDecimal(areaUtil) : null;
        return this;
    }

    Caracteristicas build() {
        return new Caracteristicas(
                quartos,
                suites,
                banheiros,
                vagas,
                areaUtil
        );
    }

}
