package io.github.igorpadua.imoveis.imovel;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Caracteristicas(int quartos, int suites, int banheiros, int vagas, BigDecimal areaUtil) {

    private static final BigDecimal AREA_MAXIMA = new BigDecimal("99999999.99");

    public Caracteristicas {

        recusaNegativo(quartos, "quartos");
        recusaNegativo(suites, "suítes");
        recusaNegativo(banheiros, "banheiros");
        recusaNegativo(vagas, "vagas");

        if (suites > quartos) {
            throw new RegraDeDominioException("Quantidade de suítes não pode ser maior que a de quartos (suítes: "
                    + suites + ", quartos: " + quartos + ").");
        }

        if (areaUtil != null) {
            if (areaUtil.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RegraDeDominioException("Área útil deve ser maior que zero (informada: "
                        + areaUtil.toPlainString() + ").");
            }

            if (areaUtil.compareTo(AREA_MAXIMA) > 0) {
                throw new RegraDeDominioException("Área útil deve ser no máximo " + AREA_MAXIMA.toPlainString()
                        + " (informada: " + areaUtil.toPlainString() + ").");
            }

            if (areaUtil.stripTrailingZeros().scale() > 2) {
                throw new RegraDeDominioException("Área útil deve ter no máximo 2 casas decimais significativas (informada: "
                        + areaUtil.toPlainString() + ").");
            }

            areaUtil = areaUtil.setScale(2, RoundingMode.UNNECESSARY);
        }

    }

    private static void recusaNegativo(int valor, String campo) {
        if (valor < 0) {
            throw new RegraDeDominioException("Quantidade de " + campo + " não pode ser negativa (informada: "
                    + valor + ").");
        }
    }

}
