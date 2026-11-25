---
paths:
  - "servicos/**"
  - "libs/**"
---

# Dinheiro e datas

- **Valores:** centavos em `long`/`bigint`, ou `BigDecimal`. Nunca `double`/`float`.
- **Comparação:** `BigDecimal` com `compareTo`, nunca com `equals` (`100` e `100.00` têm escalas diferentes). Nos testes, `isEqualByComparingTo`, a menos que a escala faça parte da regra.
- **Arredondamento:** só no valor final.
- **Competência ≠ data de pagamento:**
  - a competência (mês a que o aluguel se refere) serve para inadimplência e faturamento;
  - a data de pagamento serve para caixa, extrato e informe de rendimentos.
