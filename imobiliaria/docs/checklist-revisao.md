# Checklist de revisão

Ao revisar código, verifique sempre:

- **Tenant:** o `tenant_id` é aplicado (RLS e `@TenantId`) e o `X-Tenant-Id` é conferido contra o token.
- **Transação e outbox:** evento gravado no outbox na mesma transação do dado; nunca publicar direto no Kafka dentro da regra de negócio.
- **Consumidores:** idempotência (evento repetido) e ordem (evento com versão antiga ignorado).
- **Dinheiro:** nada de `double`/`float`; `BigDecimal` comparado com `compareTo`; arredondamento só no valor final.
- **Domínio:** regra de negócio no domínio, não no handler; fatias não chamam umas às outras.
- **API:** status HTTP e `ProblemDetail` corretos; `If-Match` e versão respeitados.
- **Padrões:** padrão aplicado onde resolve um problema real; nenhum padrão forçado onde código simples resolve.
- **Testes:** os cenários importantes da fatia estão cobertos, e cada validação do domínio tem um teste que falha sem ela.

As regras detalhadas de cada item estão em `.claude/rules/`.
