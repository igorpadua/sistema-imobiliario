---
paths:
  - "servicos/**"
  - "libs/**"
---

# Identificadores

- **Ids dos agregados:** UUID v7 (RFC 9562). É ordenado pelo tempo, o que preserva a localidade do índice B-tree.
- **Gerados no método de fábrica do agregado,** não pelo Hibernate nem pelo banco: o agregado já nasce com id, e os eventos de domínio criados na fábrica já o carregam. Com o id preenchido, o Spring Data decide entre `persist` e `merge` pelo `@Version`, que precisa ser de tipo wrapper (nulo = novo).
- **Gerador:** a JUG (`com.fasterxml.uuid:java-uuid-generator`), com `Generators.timeBasedEpochGenerator()`.
  - A instância fica num campo `static final`, porque a ordem dentro do mesmo milissegundo depende do estado dela.
  - Enquanto só um ponto gera ids, a chamada é direta; no segundo, ela é extraída para uma classe própria, o que também facilita a troca pelo gerador nativo do Java.
- **`tenant_id`:** UUID. A policy do RLS converte o `app.tenant_id` para `uuid`.
- **No Avro:** `string` com `logicalType: uuid`.
