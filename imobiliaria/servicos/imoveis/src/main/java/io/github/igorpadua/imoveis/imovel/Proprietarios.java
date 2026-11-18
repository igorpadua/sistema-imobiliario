package io.github.igorpadua.imoveis.imovel;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record Proprietarios(List<Proprietario> proprietarios) {

    private static final BigDecimal CEM = new BigDecimal("100");

    public Proprietarios {
        if (proprietarios == null) {
            throw new RegraDeDominioException("O campo proprietários é obrigatório.");
        }

        if (proprietarios.isEmpty()) {
            throw new RegraDeDominioException("O campo proprietários deve ter ao menos um proprietários.");
        }

        if (proprietarios.stream().anyMatch(Objects::isNull)) {
            throw new RegraDeDominioException("O campo proprietários não pode conter itens nulos.");
        }

        proprietarios = List.copyOf(proprietarios);

        long quantidadeDePessoasDistintas = proprietarios.stream()
                .map(Proprietario::pessoaId)
                .distinct()
                .count();

        if (quantidadeDePessoasDistintas != proprietarios.size()) {
            throw new RegraDeDominioException("Não é permitido proprietários repetidos na lista.");
        }

        BigDecimal somaPercentuais = proprietarios.stream()
                .map(Proprietario::percentual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (somaPercentuais.compareTo(CEM) != 0) {
            throw new RegraDeDominioException("A soma dos proprietários deve dar 100%.");
        }

    }

}
