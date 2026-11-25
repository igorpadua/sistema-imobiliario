package io.github.igorpadua.imoveis.imovel;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public record Proprietarios(List<Proprietario> proprietarios) {

    private static final BigDecimal CEM = new BigDecimal("100");

    public Proprietarios {
        if (proprietarios == null) {
            throw new RegraDeDominioException("O campo proprietários é obrigatório.");
        }

        if (proprietarios.isEmpty()) {
            throw new RegraDeDominioException("A lista de proprietários deve ter ao menos um proprietário.");
        }

        if (proprietarios.stream().anyMatch(Objects::isNull)) {
            throw new RegraDeDominioException("A lista de proprietários não pode conter itens nulos.");
        }

        proprietarios = List.copyOf(proprietarios);

        Set<UUID> pessoasRepetidas = proprietarios.stream()
                .collect(Collectors.groupingBy(Proprietario::pessoaId, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!pessoasRepetidas.isEmpty()) {
            throw new RegraDeDominioException("Cada pessoa pode aparecer uma única vez na lista de proprietários (pessoaId repetidos: "
                    + pessoasRepetidas.stream().map(UUID::toString).collect(Collectors.joining(", ")) + ").");
        }

        BigDecimal somaPercentuais = proprietarios.stream()
                .map(Proprietario::percentual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (somaPercentuais.compareTo(CEM) != 0) {
            throw new RegraDeDominioException("A soma dos percentuais dos proprietários deve ser exatamente 100 (informada: "
                    + somaPercentuais.toPlainString() + ").");
        }

    }

}
