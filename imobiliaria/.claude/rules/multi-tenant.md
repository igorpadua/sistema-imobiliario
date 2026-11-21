---
paths:
  - "servicos/**"
  - "libs/**"
  - "infra/postgres/**"
---

# Multi-tenant

- **Coluna `tenant_id` em todas as tabelas,** com duas barreiras:
  - `@TenantId` do Hibernate na aplicação;
  - Row Level Security no Postgres.
- **Contexto da transação:** no início de cada transação, executar `SELECT set_config('app.tenant_id', ?, true)`. O starter cuida disso. Consequência: toda leitura, inclusive com `JdbcClient`, precisa rodar dentro de uma transação (`@Transactional(readOnly = true)`); fora dela o tenant não está definido.
- **Usuários do banco:** dois roles por serviço, nenhum deles superuser nem com `BYPASSRLS`:
  - o dono das tabelas, usado só pelo Flyway (`spring.flyway.user`);
  - o da aplicação, só com permissão de leitura e escrita de dados.
- **`FORCE ROW LEVEL SECURITY`** em todas as tabelas. Superuser e roles com `BYPASSRLS` sempre ignoram o RLS, e o `FORCE` só tem efeito sobre o dono da tabela. Como o dono também fica sob RLS, o DDL das migrações funciona normalmente, mas uma migração de dados precisa definir o tenant explicitamente.
- **Unicidade:** sempre por tenant, por exemplo `UNIQUE (tenant_id, codigo)`. Não existe entidade global entre tenants.
