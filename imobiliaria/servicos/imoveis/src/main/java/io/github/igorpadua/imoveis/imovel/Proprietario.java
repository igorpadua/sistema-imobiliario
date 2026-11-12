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
            throw new RegraDeDominioException("Não é permitido percentual nulo.");
        }

        if (percentual.signum() != 1) {
            throw new RegraDeDominioException("Só é aceito percentual acima de 0.");
        }

        if (percentual.stripTrailingZeros().scale() > 4) {
            throw new RegraDeDominioException("Percentual pode ter no máximo 4 casas.");
        }

        percentual = percentual.setScale(4, RoundingMode.UNNECESSARY);

    }

}
