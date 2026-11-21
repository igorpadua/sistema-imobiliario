---
paths:
  - "**/src/test/**"
---

# Testes

- **Teste antes do código (TDD), um cenário por vez:** escrever o cenário, ver o teste falhar, implementar o mínimo para ele passar e limpar o código com tudo verde.
  - Vale para o domínio e para as fatias. Na fatia o ciclo é mais lento, por causa do container, mas a ordem é a mesma.
  - Toda validação do domínio nasce de um teste vermelho. Por isso, remover uma validação faz algum teste falhar.
- **Domínio:** testes unitários puros, com `@ParameterizedTest` (com `name` legível) e `@CsvSource`/`@MethodSource`. Montar dados com Test Data Builders (`umImovel()...build()`). É onde fica a maior parte dos testes.
- **Fatia:** um teste de integração por caso de uso: MockMvc, Postgres via Testcontainers e verificação do evento no outbox. Usar `@Nested` para agrupar os cenários.
- **Persistência:** sempre Postgres real, nunca H2. Teste obrigatório: o RLS impede leitura entre tenants, verificado com `JdbcTemplate` (que ignora o filtro do Hibernate).
- **Usuário nos testes:** os testes conectam com o role da aplicação, não com o usuário padrão do container do Testcontainers, que é superuser e ignora o RLS. Com ele, os testes passariam sem exercitar o isolamento entre tenants.
- **Web e segurança:** `jwt()` do `spring-security-test`, com a configuração de segurança importada. Cobrir 401, 403 (tenant fora do token, papel sem permissão), 412 e 422.
- **Kafka:** Testcontainers com Awaitility, e `schema.registry.url=mock://teste`. Testar:
  - produção com o header `tenantId`;
  - idempotência (evento duplicado);
  - eventos fora de ordem.
- **Dublês:**
  - sem fake de repositório: os handlers são cobertos pelo teste de integração da fatia, com banco real;
  - fake em memória para o publicador de eventos;
  - Mockito só para bordas externas, com `@MockitoBean`.
- **ArchUnit:** agregados e value objects (classes na raiz de cada área, exceto as interfaces de repositório) não dependem de `org.springframework..`; uma fatia não depende de outra fatia.
- **Containers compartilhados:** declarados uma vez, como `static`, com `@ServiceConnection`.
