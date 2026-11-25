---
paths:
  - "servicos/**"
  - "libs/**"
---

# API REST

- **Erros** em `ProblemDetail` (RFC 9457):
  - 422 para violação de regra de domínio;
  - 412 para `If-Match` com versão desatualizada;
  - 401 sem token; 403 sem permissão ou com tenant inválido.
- **Concorrência:** locking otimista em dois níveis. Comparação explícita da versão enviada pelo cliente (gera o 412) e `@Version` do JPA (pega a corrida entre leitura e escrita).
- **Operações longas** (relatórios pesados): `202` com `jobId`.
