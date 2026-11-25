package io.github.igorpadua.imoveis.imovel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public record Proprietario(UUID pessoaId, BigDecimal percentual) {

    public Proprietario {

        if (pessoaId == null) {
            throw new RegraDeDominioException("O campo pessoaId é obrigatório.");
        }

        if (percentual == null) {
            throw new RegraDeDominioException("O campo percentual é obrigatório.");
        }

        if (percentual.signum() != 1) {
            throw new RegraDeDominioException("Percentual deve ser maior que zero (informado: "
                    + percentual.toPlainString() + ").");
        }

        if (percentual.stripTrailingZeros().scale() > 4) {
            throw new RegraDeDominioException("Percentual deve ter no máximo 4 casas decimais significativas (informado: "
                    + percentual.toPlainString() + ").");
        }

        percentual = percentual.setScale(4, RoundingMode.UNNECESSARY);

    }

}
