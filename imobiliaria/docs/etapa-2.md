# Etapa 2 — Imóveis

Primeiro serviço: agregado `Imovel` com RLS, validação de token (testada com `jwt()`, ainda sem Keycloak), outbox e publicação em Avro no Kafka. Depois, proprietários, `Oferta` e busca.

Fora desta etapa: o listener `contratoativado` (depende de Contratos, etapa 5), a mídia (depois do marco), Keycloak e Traefik (etapa 3), a extração do starter e dos convention plugins (etapa 4).

## Definição de pronto

- [ ] Um teste cria um imóvel pela API (com `jwt()`) e consome a mensagem no Kafka: chave igual ao id do imóvel, header `tenantId`, valor em Avro.
- [ ] O RLS impede leitura e escrita entre tenants, verificado com `JdbcTemplate` conectado como role da aplicação.
- [ ] Toda fatia tem teste de integração com 401, 403 e os status de erro que se aplicam a ela.
- [ ] As regras do ArchUnit passam.
- [ ] `./gradlew build` passa do zero, só com o Docker rodando.

Conferir o evento no Kafka UI com o serviço rodando contra o compose exige um token real, então fica para a etapa 3.

## Ordem

- **T1 a T9** são o caminho mais curto até o primeiro evento no Kafka: uma fatia atravessando todas as camadas.
- **T10 a T13** completam a área na mesma estrutura. Cada fatia nova já nasce gravando o seu evento no outbox.
- **T14** fecha a etapa.
- Cada dependência do Gradle entra na tarefa que precisa dela: Testcontainers na T2, segurança na T5, ArchUnit na T6, Avro na T8, Kafka na T9.

## Decisões pendentes

Itens de **Decisões em aberto** do `CLAUDE.md`, na ordem em que bloqueiam as tarefas. Ao decidir, mova o item para a seção correspondente do `CLAUDE.md`.

- [ ] **Status HTTP restantes.**
  - Antes da T4: erro de formato ou validação, e `X-Tenant-Id` ausente ou que não é UUID. Recomendação: 400. A requisição nem chega ao domínio, então não é 422. O 403 fica para tenant válido que não está no token.
  - Antes da T10: conflito pego pelo `@Version`. Recomendação: 412. Para o cliente, as duas situações (versão velha no `If-Match` e corrida) pedem a mesma ação: recarregar e tentar de novo.
  - Antes da T10: requisição sem `If-Match`. Recomendação: 428 (RFC 6585), para nenhuma escrita passar sem controle de concorrência.
- [ ] **Fake do publicador de eventos** (antes da T7). Recomendação: sem fake. O teste da fatia lê a tabela de outbox, e o teste de domínio verifica os eventos que o agregado registra. Se decidir assim, ajuste a seção **Testes** do `CLAUDE.md`.
- [ ] **Envelope do evento** (antes da T7). Recomendação:
  - campo `tipo` com o nome do evento de domínio (ex.: `ImovelCadastrado`);
  - `versao` igual ao `@Version` do agregado depois da gravação;
  - um evento de integração por agregado por transação: vários eventos de domínio no mesmo comando viram um único snapshot;
  - eventos sem agregado ficam para a etapa 7, quando o `PagamentoRecebido` aparece.
- [ ] **Datas e fuso** (só o `ocorridoEm`, antes da T7). Recomendação: `Instant` em UTC, `timestamptz` no Postgres e `timestamp-micros` no Avro, que tem a mesma precisão do Postgres. Competência e fuso dos jobs ficam para a etapa 6.
- [ ] **Local dos schemas Avro** (antes da T8). Recomendação: dentro do Imóveis, extraídos para `libs/eventos-schemas` na etapa 4, quando o Pessoas consumir. É a regra "extrair quando repetir".
- [ ] **Vários tipos de evento por tópico** (antes da T8). Com snapshot por agregado, `imoveis.v1` terá dois tipos: o do imóvel e o da oferta (T12). Recomendação: `TopicRecordNameStrategy`, com um subject por par (tópico, tipo), compatibilidade verificada por tipo e sem union para manter. Custo: nenhum schema descreve o tópico inteiro.
- [ ] **Modo de compatibilidade** (antes da T8). Recomendação: `FULL_TRANSITIVE`. O replay lê eventos antigos com o schema novo, e consumidores ainda não atualizados leem eventos novos, contra todas as versões. Para o build, prefira verificar contra as versões anteriores guardadas no repositório, sem depender do Registry no ar.
- [ ] **Outbox e jobs × RLS** (antes da T9). A execução por tenant não é viável agora: a lista de tenants só existe na etapa 12. Além das opções do `CLAUDE.md`, há uma quarta: um role do publicador, sem `BYPASSRLS`, com uma policy própria (`CREATE POLICY ... TO <role>`) só na tabela de outbox e sem acesso às tabelas de negócio. Recomendação: a quarta, porque o isolamento continua no banco para todo o resto. Custo: um segundo `DataSource` na aplicação e um role novo no script de init.
- [ ] **Escala dos percentuais** (antes da T11). Recomendação: duas casas (`numeric(5,2)`) e soma exata de 100,00. Quem cadastra resolve o resto (33,33 + 33,33 + 33,34).
- [ ] **O que é compartilhado entre áreas** (antes da T12). Recomendação: a `Oferta` guarda só o id do imóvel, e o `Endereco` fica em `imovel/` até outra área precisar dele. Se publicar uma oferta depender da situação do imóvel, o handler lê o `Imovel` pelo repositório público e passa o dado para a `Oferta`.
- [ ] **Representação de dinheiro** (antes da T12). Recomendação: `BigDecimal` com escala 2 de ponta a ponta (`numeric` no Postgres, `decimal` no Avro). Multa, juros e rateio (etapas 6 e 8) vão calcular em `BigDecimal` de qualquer jeito, e assim o arredondamento fica só no valor final. Custo: a escala fica fixa no schema, e o gerador de classes precisa ser configurado para `BigDecimal`.

## Lacunas encontradas

Não estão em **Decisões em aberto**, mas bloqueiam tarefas desta etapa. Proposta: registrar no `CLAUDE.md`.

- **Ciclo da `situacao`** (antes da T1): o que o imóvel precisa ter para sair de RASCUNHO (ex.: proprietários somando 100) e se INATIVO pode voltar a ATIVO.
- **Papéis por operação** (antes da T5): quais papéis podem cadastrar, ativar, definir proprietários, publicar oferta e buscar.
- **Índice parcial e RESERVADA da `Oferta`** (antes da T12): está listado em **Venda**, mas a T12 já precisa saber quais status contam no índice único e quando uma oferta fica RESERVADA.
- **`pessoaId` dos proprietários** (antes da T11): até a etapa 4 não existe Pessoas, então o Imóveis aceita qualquer UUID. Definir se, na etapa 4, ele passa a manter uma cópia de `pessoas.v1` para validar.

---

## T1. Agregado `Imovel` (domínio puro)

**Decisões antes de começar:** a lacuna do ciclo da `situacao`. O gerador de UUID v7 é escolhido nesta tarefa, como o `CLAUDE.md` prevê.

**Critérios de aceite**

- [ ] O `Imovel` só é criado por um método de fábrica estático e já nasce com id v7, `situacao` RASCUNHO e `ocupacao` VAGO.
- [ ] `Endereco` é um record que se valida no construtor, incluindo os limites de latitude e longitude.
- [ ] Violação de invariante lança uma exceção de domínio (que vira 422 na API).
- [ ] As transições de `situacao` seguem o ciclo decidido, e as inválidas são rejeitadas.
- [ ] A fábrica registra o evento de domínio `ImovelCadastrado` (record), já com o id.
- [ ] Testes unitários sem Spring, com `@ParameterizedTest` (com `name` legível) e um Test Data Builder `umImovel()`.

**Pontos de atenção**

- As anotações JPA entram na T3. Aqui o foco é a regra.
- O código legível vem de uma tabela do banco (T4). A fábrica recebe o código pronto e continua pura.
- `AbstractAggregateRoot` e `@DomainEvents` são do Spring Data: usá-los no agregado viola a regra do ArchUnit (T6).
- O `java.util.UUID` do Java 25 não gera v7 (o suporte está previsto para o JDK 26; confirme nas notas de versão). Bibliotecas como `uuid-creator` e `java-uuid-generator` geram.
- Defina o conjunto mínimo de características. Ele pode crescer depois.

**Documentação**

- UUID v7: https://www.rfc-editor.org/rfc/rfc9562#name-uuid-version-7
- Testes parametrizados: https://docs.junit.org/current/user-guide/#writing-tests-parameterized-tests

---

## T2. Ambiente de teste com Postgres real e os dois roles

**Decisões antes de começar:** nenhuma.

**Critérios de aceite**

- [ ] Um container Postgres compartilhado (`static`), com a mesma imagem do compose.
- [ ] O script `infra/postgres/docker-entrypoint-initdb.d/01-imoveis.sql` é reaproveitado, sem cópia, e cria no container o database, os roles e as extensões.
- [ ] O Flyway conecta como `imoveis_dono`, e a aplicação como `imoveis_app`.
- [ ] Um teste confere, pela conexão da aplicação, que `current_user` é `imoveis_app` e que ele tem `rolsuper` e `rolbypassrls` falsos.
- [ ] O `application.yaml` aponta para o Postgres do compose com os mesmos dois roles, e as senhas vêm de variáveis de ambiente.

**Pontos de atenção**

- O usuário padrão do container é superuser e ignora o RLS. O `withUsername` não resolve: ele vira o `POSTGRES_USER` da imagem, que também é superuser.
- O `withInitScript` executa o script via JDBC, não pelo `psql`, então os meta-comandos do script (`\getenv`, `\connect`) não funcionam por ele. A imagem roda sozinha o que estiver em `/docker-entrypoint-initdb.d`, e o script lê as senhas de variáveis de ambiente do container.
- O Testcontainers só aceita a imagem do PostGIS se ela for declarada como substituta compatível de `postgres`.
- O `@ServiceConnection` registra os dados de conexão do container, usuário incluso, e eles têm precedência sobre `spring.datasource.*`. O `CLAUDE.md` pede `@ServiceConnection`: descubra se ele combina com o role da aplicação. Se não combinar, registre a exceção no `CLAUDE.md`.
- O Spring Boot 4 usa o Testcontainers 2, que renomeou os artefatos (ex.: `testcontainers-postgresql`). Tutoriais antigos usam os nomes da 1.x.
- O compose cria o servidor com ordenação ICU `pt-BR`. Se o container de teste não fizer o mesmo, a ordenação da busca (T13) muda.

**Documentação**

- Módulo Postgres do Testcontainers: https://java.testcontainers.org/modules/databases/postgres/
- Testcontainers no Spring Boot (service connections): https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
- Flyway no Spring Boot: https://docs.spring.io/spring-boot/how-to/data-initialization.html

---

## T3. Tabela `imovel`, mapeamento JPA e RLS

**Decisões antes de começar:** nenhuma.

**Critérios de aceite**

- [ ] Uma migração cria a tabela com `tenant_id uuid not null`, unicidade `(tenant_id, codigo)`, `ENABLE` e `FORCE ROW LEVEL SECURITY` e uma policy com `USING` e `WITH CHECK`.
- [ ] O agregado ganha `@Entity`, `@TenantId` e `@Version` (wrapper), e o `Endereco` vira `@Embeddable`.
- [ ] O tenant vem de um contexto (por enquanto preenchido pelos testes) e é lido pelo `CurrentTenantIdentifierResolver`.
- [ ] Toda transação começa com `set_config('app.tenant_id', ?, true)`, com o tenant do contexto.
- [ ] Salvar e carregar pelo repositório funciona, e o Hibernate preenche o `tenant_id`.
- [ ] Testes de RLS com `JdbcTemplate`:
  - [ ] com o tenant B ativo, a linha gravada pelo tenant A não aparece;
  - [ ] gravar com um `tenant_id` diferente do tenant ativo é rejeitado;
  - [ ] sem tenant definido, nenhuma linha aparece e nada quebra, inclusive numa conexão do pool que já teve tenant antes.

**Pontos de atenção**

- Depois que `app.tenant_id` é definido uma vez numa conexão, `current_setting('app.tenant_id', true)` passa a devolver `''`, e não `NULL`, quando a transação termina. Com pool, a próxima transação sem tenant tenta converter `''` para `uuid` e quebra. Pense em como a policy trata isso e continua fechada.
- O terceiro parâmetro `true` limita o valor à transação. Com `false`, o tenant vaza para a próxima requisição que pegar a mesma conexão.
- Pesquise como o `JpaTransactionManager` abre a transação e expõe a conexão JDBC. O mesmo ponto precisa valer para leituras com `JdbcClient` (T13).
- Para o contexto do tenant, compare `ThreadLocal` e `ScopedValue` (final no Java 25), principalmente em como cada um é limpo no fim da requisição.
- A `flyway_schema_history` é do Flyway e não tem `tenant_id`. O RLS é para as tabelas de negócio.
- O `@Version` precisa ser wrapper: com o id preenchido pela fábrica, é ele que diz ao Spring Data se a entidade é nova.

**Documentação**

- RLS: https://www.postgresql.org/docs/current/ddl-rowsecurity.html
- `CREATE POLICY`: https://www.postgresql.org/docs/current/sql-createpolicy.html
- `set_config` e `current_setting`: https://www.postgresql.org/docs/current/functions-admin.html
- Discussão sobre o `''` depois da transação: https://www.postgresql.org/message-id/CA%2BQ86ij0KDCB0G45G509-8q0DNR611gcKG-sSM83GA1EBL7boA%40mail.gmail.com
- `@TenantId` (seção de multitenancy do guia do Hibernate): https://hibernate.org/orm/documentation/
- Entidade nova ou existente no Spring Data: https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html
- `ScopedValue`: https://openjdk.org/jeps/506

---

## T4. Fatia `cadastrar`

**Decisões antes de começar:** status HTTP de formato e validação.

**Critérios de aceite**

- [ ] `POST /imoveis`, com request e response como records e as classes da fatia package-private.
- [ ] O código legível vem de uma tabela de sequência por tenant, também com RLS. Dois cadastros concorrentes no mesmo tenant não repetem o código; tenants diferentes podem ter o mesmo código.
- [ ] Resposta 201 com `Location` e `ETag` com a versão.
- [ ] Erros em `ProblemDetail`: entrada malformada ou inválida com o status decidido, regra de domínio com 422.
- [ ] O tenant vem do `X-Tenant-Id`. É provisório: a T5 passa a conferi-lo contra o token.
- [ ] Teste de integração da fatia com MockMvc e Postgres real, com os cenários agrupados em `@Nested`.

**Pontos de atenção**

- O handler só orquestra: gera o código, chama a fábrica e salva.
- A linha do tenant na tabela de sequência pode ainda não existir. Pesquise um comando SQL único que crie ou incremente a linha e devolva o valor, seguro sob concorrência.
- Uma `SEQUENCE` do Postgres é global, não por tenant, e o `nextval` não volta em rollback.
- O valor do `ETag` vai entre aspas.
- O Spring MVC tem suporte nativo a `ProblemDetail`. Veja como ligá-lo e como mapear a exceção de domínio.

**Documentação**

- Erros em `ProblemDetail` no Spring MVC: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html
- RFC 9457: https://www.rfc-editor.org/rfc/rfc9457
- `ETag`: https://www.rfc-editor.org/rfc/rfc9110#name-etag
- `INSERT` (veja `ON CONFLICT` e `RETURNING`): https://www.postgresql.org/docs/current/sql-insert.html

---

## T5. Segurança: token, tenant e papéis

**Decisões antes de começar:** a lacuna dos papéis por operação.

**Critérios de aceite**

- [ ] Toda rota de negócio exige um JWT válido (assinatura, `iss`, `aud`, `exp`). Sem token, 401.
- [ ] O `X-Tenant-Id` só é aceito se o tenant estiver no claim `tenants` (`/tenants/{tenantId}/{papel}`). Fora do token, 403 em `ProblemDetail`.
- [ ] Os papéis valem só para o tenant ativo: quem é `admin` no tenant A e `corretor` no B, chamando com o B, tem só `corretor`.
- [ ] O `cadastrar` exige um dos papéis decididos. Com outro papel, 403.
- [ ] O contexto do tenant (T3) só recebe o tenant depois dessa checagem.
- [ ] Testes com `jwt()` e a configuração de segurança importada, cobrindo 401, os dois tipos de 403 e o usuário em dois tenants.

**Pontos de atenção**

- Sem o Keycloak no ar, o contexto ainda precisa subir. Configure o resource server como na armadilha do Docker do `CLAUDE.md` (`jwk-set-uri` interno e issuer validado explicitamente) e confira se o contexto sobe sem o Keycloak.
- O conversor de autoridades recebe só o `Jwt`, não a requisição, mas os papéis dependem do header. Pense em onde o token e o header estão disponíveis juntos.
- O Spring Boot tem uma propriedade para validar a audiência no resource server. Confira o nome na documentação.
- Decida se o health do Actuator fica aberto: o healthcheck do compose vai precisar dele.

**Documentação**

- Resource server com JWT: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- Testes com `jwt()`: https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html
- Propriedades do resource server no Spring Boot: https://docs.spring.io/spring-boot/reference/web/spring-security.html

---

## T6. Testes de arquitetura

**Decisões antes de começar:** nenhuma.

**Critérios de aceite**

- [ ] As classes na raiz de cada área, exceto as interfaces de repositório, não dependem de `org.springframework..`.
- [ ] Uma fatia não depende de outra fatia.
- [ ] Cada regra falha quando violada: crie uma dependência proibida, veja o teste falhar e desfaça.

**Pontos de atenção**

- As fatias são subpacotes das áreas. Pesquise as regras de `slices()` do ArchUnit.
- Anotações de `jakarta.persistence` e `org.hibernate` continuam permitidas no agregado. A regra barra só o Spring.

**Documentação**

- Guia do ArchUnit: https://www.archunit.org/userguide/html/000_Index.html

---

## T7. Outbox: gravação na mesma transação

**Decisões antes de começar:** fake do publicador, envelope do evento e datas (`ocorridoEm`).

**Critérios de aceite**

- [ ] A tabela de outbox tem `tenant_id`, o id do agregado (futura chave da mensagem), o tipo, o payload, o `ocorridoEm` e o estado de publicação, sob RLS como as demais.
- [ ] O `cadastrar` grava o evento na mesma transação do imóvel. Um teste prova que, se a transação falhar, nem o imóvel nem o evento ficam gravados.
- [ ] O evento carrega o snapshot completo do imóvel, `eventoId`, `versao` e `ocorridoEm`, e a `versao` bate com o `@Version` gravado.
- [ ] O mapeamento de evento de domínio para evento de integração fica em `infra/`.
- [ ] O teste da fatia `cadastrar` verifica o evento no outbox.

**Pontos de atenção**

- O `@Version` só muda no flush. Um evento montado antes disso leva a versão errada.
- Decida o que a linha guarda. Serializar em Avro com o Schema Registry dentro da transação faz a gravação depender do Registry estar no ar.
- O `Instant` do Java tem nanossegundos, e o `timestamptz` guarda microssegundos. Comparações nos testes falham se o valor não for truncado.
- Nada de `KafkaTemplate` no handler nem no domínio.

**Documentação**

- Transactional Outbox: https://microservices.io/patterns/data/transactional-outbox.html
- Transações no Spring: https://docs.spring.io/spring-framework/reference/data-access/transaction.html

---

## T8. Schemas Avro e geração de classes

**Decisões antes de começar:** local dos schemas, vários tipos por tópico e modo de compatibilidade.

**Critérios de aceite**

- [ ] O schema do evento do imóvel está em `.avsc`, com ids como `string` e `logicalType: uuid` e o `ocorridoEm` com o logical type de tempo decidido.
- [ ] O build gera as classes Java a partir do `.avsc`.
- [ ] O build falha quando uma mudança quebra a compatibilidade decidida. Teste: adicione um campo sem default, veja falhar e desfaça.
- [ ] A estratégia de subject e o modo de compatibilidade estão configurados.

**Pontos de atenção**

- O plugin Gradle de Avro mais citado em tutoriais (`davidmc24/gradle-avro-plugin`) foi arquivado, e a última versão é de 2023. Procure uma alternativa mantida ou uma task que chame o compilador do próprio Avro.
- Os serializers da Confluent não estão no Maven Central: o build precisa do repositório Maven da Confluent.
- O serializer registra o schema sozinho por padrão (`auto.register.schemas`). Decida se o produtor pode fazer isso.
- O próprio Avro tem classes para checar compatibilidade entre versões de schema, sem o Registry.

**Documentação**

- Especificação do Avro (logical types, resolução de schemas): https://avro.apache.org/docs/
- Serializers e estratégias de subject: https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/index.html
- Modos de compatibilidade: https://docs.confluent.io/platform/current/schema-registry/fundamentals/schema-evolution.html

---

## T9. Publicador do outbox

**Decisões antes de começar:** outbox e jobs × RLS.

**Critérios de aceite**

- [ ] Um publicador agendado lê os eventos pendentes e publica em `imoveis.v1`, com a chave igual ao id do agregado, o header `tenantId` e o valor em Avro.
- [ ] O evento só é marcado como publicado depois da confirmação do broker. Com o Kafka fora, ele continua pendente e sai quando o Kafka volta.
- [ ] Eventos do mesmo agregado saem na ordem em que foram gravados.
- [ ] A solução de RLS decidida está aplicada, inclusive no script de init se houver role novo.
- [ ] Teste com Kafka via Testcontainers, `schema.registry.url=mock://teste` e Awaitility: cria o imóvel pela API e consome a mensagem, conferindo chave, header e conteúdo. Este teste é a definição de pronto da etapa.

**Pontos de atenção**

- A entrega é "pelo menos uma vez": publicar e cair antes de marcar gera duplicata. Por isso os consumidores (etapa 4) são idempotentes.
- Com mais de uma instância do publicador, eventos do mesmo agregado podem sair fora de ordem. Com uma instância, basta ordenar a leitura. Registre a limitação.
- Confira os padrões do produtor Kafka para idempotência e `acks`.
- Com `mock://`, o registry é um mock em memória por escopo: o produtor e o consumidor do teste precisam usar a mesma URL.
- Decida o que acontece com os eventos já publicados (apagar ou manter), porque a tabela cresce.

**Documentação**

- Spring for Apache Kafka: https://docs.spring.io/spring-kafka/reference/
- Agendamento no Spring: https://docs.spring.io/spring-framework/reference/integration/scheduling.html
- Módulo Kafka do Testcontainers: https://java.testcontainers.org/modules/kafka/
- Awaitility: https://github.com/awaitility/awaitility/wiki/Usage

---

## T10. Fatias `consultar`, `ativar` e `inativar`, com concorrência

**Decisões antes de começar:** status HTTP do conflito do `@Version` e da falta de `If-Match`.

**Critérios de aceite**

- [ ] `GET /imoveis/{id}` devolve o imóvel com `ETag`. Um id de outro tenant responde como inexistente (404).
- [ ] Ativar e inativar exigem `If-Match`. Versão desatualizada dá 412, transição inválida dá 422, e a falta do header dá o status decidido.
- [ ] A corrida entre leitura e escrita é pega pelo `@Version` e vira o status decidido. O teste usa duas transações concorrentes.
- [ ] Cada transição grava o seu evento no outbox, e o teste da fatia confere.

**Pontos de atenção**

- São dois níveis: a comparação explícita com o `If-Match` e o `@Version`. Sem a comparação explícita, o `@Version` compara com a versão que a própria transação acabou de ler, e a versão do cliente é ignorada.
- O `ShallowEtagHeaderFilter` do Spring gera o `ETag` a partir do hash do corpo. Não é o que o projeto quer.

**Documentação**

- Requisições condicionais e `If-Match`: https://www.rfc-editor.org/rfc/rfc9110#name-conditional-requests
- 428 Precondition Required: https://www.rfc-editor.org/rfc/rfc6585#section-3

---

## T11. Fatia `definirproprietarios`

**Decisões antes de começar:** escala dos percentuais e a lacuna do `pessoaId`.

**Critérios de aceite**

- [ ] `PUT /imoveis/{id}/proprietarios` substitui a lista inteira e exige `If-Match`.
- [ ] A soma é exatamente 100, comparada com `compareTo`. Cada percentual é positivo, e a mesma pessoa não aparece duas vezes. Violação dá 422.
- [ ] Teste de domínio parametrizado com casos como 50 + 50, 33,33 + 33,33 + 33,34, 33,33 × 3, percentual com casas a mais e pessoa repetida.
- [ ] A tabela dos proprietários tem `tenant_id` e RLS.
- [ ] O schema do evento evolui de forma compatível para levar os proprietários, e o build confirma.

**Pontos de atenção**

- O `@TenantId` atua em entidades. Confira o que acontece com a tabela de uma coleção (`@ElementCollection`), que também precisa de `tenant_id` para o RLS.
- Confira se alterar só a coleção incrementa o `@Version` do imóvel.
- `BigDecimal.equals` compara a escala: `100` é diferente de `100.00`.

**Documentação**

- `BigDecimal.compareTo`: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/math/BigDecimal.html#compareTo(java.math.BigDecimal)
- Evolução de schemas: https://docs.confluent.io/platform/current/schema-registry/fundamentals/schema-evolution.html

---

## T12. Agregado `Oferta`: `publicar`, `alterarpreco` e `encerrar`

**Decisões antes de começar:** o que é compartilhado entre áreas, representação de dinheiro e a lacuna do índice parcial e da RESERVADA.

**Critérios de aceite**

- [ ] A área `oferta/` tem agregado, repositório, fábrica e eventos próprios.
- [ ] No máximo uma oferta viva por imóvel e modalidade, garantida por índice único parcial. Duas publicações simultâneas resultam numa oferta e num erro HTTP adequado, não num 500.
- [ ] Alterar o preço encerra a oferta atual e cria outra, o que preserva o histórico.
- [ ] Preço positivo, sem `double` nem `float`.
- [ ] A tabela tem `tenant_id` e RLS.
- [ ] Os eventos da oferta saem em `imoveis.v1`, com a estratégia de subject decidida.

**Pontos de atenção**

- Alterar o preço muda dois agregados na mesma transação. Confira se isso respeita a decisão de um evento por agregado por transação.
- O logical type `decimal` do Avro vira `ByteBuffer` nas classes geradas, a menos que o gerador seja configurado para `BigDecimal`.
- `alterarpreco` não chama `publicar`. O que for comum vai para a raiz da área.

**Documentação**

- Índices parciais: https://www.postgresql.org/docs/current/indexes-partial.html
- Decimal no Avro: https://avro.apache.org/docs/

---

## T13. Fatia `buscarimoveis`

**Decisões antes de começar:** nenhuma.

**Critérios de aceite**

- [ ] `GET /imoveis` com filtros (texto aproximado com `pg_trgm`, raio a partir de um ponto com PostGIS, tipo e situação), paginação e ordenação.
- [ ] A leitura usa `JdbcClient` dentro de `@Transactional(readOnly = true)`.
- [ ] Um teste prova que a busca nunca devolve imóvel de outro tenant. Aqui quem protege é só o RLS.
- [ ] Índices trigram e espacial criados. Com volume suficiente, o `EXPLAIN` mostra os índices em uso.
- [ ] A ordenação por texto segue o `pt-BR` (acentos).

**Pontos de atenção**

- A coluna espacial pode ser mapeada no JPA (Hibernate Spatial) ou existir só no banco, gerada a partir da latitude e da longitude. Compare.
- `geography` mede em metros; `geometry` mede nas unidades do SRID.
- No `ST_MakePoint`, x é a longitude e y é a latitude.
- Com poucas linhas, o Postgres prefere varrer a tabela, então o `EXPLAIN` não mostra os índices.

**Documentação**

- `pg_trgm`: https://www.postgresql.org/docs/current/pgtrgm.html
- `ST_DWithin`: https://postgis.net/docs/ST_DWithin.html
- Colunas geradas: https://www.postgresql.org/docs/current/ddl-generated-columns.html
- `JdbcClient`: https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html#jdbc-JdbcClient

---

## T14. Fechamento da etapa

**Critérios de aceite**

- [ ] A definição de pronto, no início deste arquivo, funciona.
- [ ] Revisão de fechamento: o implementado bate com o `CLAUDE.md`, e as lacunas descobertas estão registradas.
- [ ] As decisões tomadas saíram de **Decisões em aberto** e foram para as seções correspondentes do `CLAUDE.md`.
- [ ] O status da etapa está atualizado no `CLAUDE.md`.
- [ ] As regras de negócio e as decisões em aberto da etapa 3 foram revisadas, e as que bloqueiam o início dela (roteamento no Traefik e armazenamento do Keycloak) estão decididas.
