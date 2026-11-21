package io.github.igorpadua.imoveis.imovel;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Caracteristicas(int quartos, int suites, int banheiros, int vagas, BigDecimal areaUtil) {

    private static final BigDecimal AREA_MAXIMA = new BigDecimal("99999999.99");

    public Caracteristicas {

        recusaNegativo(quartos, "Quartos");
        recusaNegativo(suites, "Suíte");
        recusaNegativo(banheiros, "Banheiro");
        recusaNegativo(vagas, "Vaga");

        if (suites > quartos) {
            throw new RegraDeDominioException("A quantidade de suítes não pode ser maior que a quantidade de quartos.");
        }

        if (areaUtil != null) {
            if (areaUtil.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RegraDeDominioException("A área não pode ser zero ou negativa.");
            }

            if (areaUtil.compareTo(AREA_MAXIMA) > 0) {
                throw new RegraDeDominioException("A área não pode ser maior que " + AREA_MAXIMA.toPlainString() + " (informado: "
                        + areaUtil.toPlainString() + ").");
            }

            if (areaUtil.stripTrailingZeros().scale() > 2) {
                throw new RegraDeDominioException("Área deve ter no máximo 2 casas decimais significativas (informado: "
                        + areaUtil.toPlainString() + ").");
            }

            areaUtil = areaUtil.setScale(2, RoundingMode.UNNECESSARY);
        }

    }

    private static void recusaNegativo(int valor, String campo) {
        if (valor < 0) {
            throw new RegraDeDominioException("O campo " + campo + " não pode ser negativo.");
        }
    }

}
