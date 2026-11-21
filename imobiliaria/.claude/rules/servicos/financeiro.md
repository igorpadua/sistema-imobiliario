---
paths:
  - "servicos/financeiro/**"
---

# Financeiro (a detalhar)

Responsabilidade: parcelas em janela móvel, multa e juros, baixa, pagamento parcial, acerto final, caução.

- **Parcelas em janela móvel** (próximos meses), nunca o contrato inteiro de uma vez.
- **Multa e juros** parametrizados por contrato; tratar pagamento parcial e pagamento a maior.
- **Acerto final** a partir de `EncerramentoIniciado`; devolução da caução com os rendimentos da poupança.
