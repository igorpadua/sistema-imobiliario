# Etapa 1 — Infraestrutura base

Docker Compose com Postgres (PostGIS, um database por serviço), Kafka (KRaft), Confluent Schema Registry e Kafka UI. Keycloak e Traefik ficam para a etapa 3.

## Definição de pronto

- [ ] A partir do zero, `docker compose down -v && docker compose up -d` deixa todos os serviços `healthy`, sem nenhum passo manual.
- [ ] É possível produzir e consumir num tópico de teste.
- [ ] O Schema Registry responde.
- [ ] O database `imoveis` existe, com os roles e as extensões.

## Decisão pendente

- [x] **Criação automática de tópicos** (antes da T3): ligada ou desligada no broker. Recomendação: desligada, para um erro de digitação não criar um tópico silenciosamente. **Decidido: ligada**, por praticidade no desenvolvimento. Registrado no `CLAUDE.md`, seção **Mensageria**.

## Ordem

As trilhas Postgres (T1 → T2) e Kafka (T3 → T4 → T5) são independentes. A T6 fecha a etapa.

---

## T1. Postgres com PostGIS

**Critérios de aceite**

- [x] O compose fica em `infra/`.
- [x] A imagem tem tag fixa (nada de `latest`).
- [x] A senha do superuser vem de um `.env`.
- [x] Existe um healthcheck, e o container fica `healthy`.
- [x] Um volume nomeado preserva os dados entre `down` e `up`.

**Pontos de atenção**

- Se usar Postgres 18, confira na página da imagem o caminho de montagem do volume. A imagem oficial mudou isso na versão 18.
- Para o healthcheck, veja o `pg_isready`.

**Documentação**

- Imagem PostGIS: https://hub.docker.com/r/postgis/postgis
- Imagem oficial do Postgres (variáveis e volume): https://hub.docker.com/_/postgres
- Healthcheck no Compose: https://docs.docker.com/reference/compose-file/services/#healthcheck

---

## T2. Script de init: database, roles e extensões do Imóveis

**Critérios de aceite**

- [x] Existe o database `imoveis`.
- [x] Existem dois roles do serviço: o dono (usado só pelo Flyway) e o da aplicação.
- [x] `SELECT rolname, rolsuper, rolbypassrls FROM pg_roles` mostra os dois com `false` nas duas colunas.
- [x] O dono é dono do database.
- [x] Uma tabela criada pelo dono pode ser lida e escrita pelo role da aplicação, sem `GRANT` manual. Teste com uma tabela temporária e apague depois.
- [x] O role da aplicação **não** consegue criar tabelas.
- [x] `CONNECT` foi revogado de `PUBLIC` no database `imoveis`.
- [x] As extensões `postgis` e `pg_trgm` estão criadas em `imoveis`.

**Pontos de atenção**

- Scripts em `/docker-entrypoint-initdb.d` **só rodam com o volume vazio**. Para rodar de novo, use `docker compose down -v`.
- O Postgres 15 mudou as permissões do schema `public`.
- Pesquise "default privileges" para o critério da tabela criada pelo dono.
- Sem revogar o `CONNECT` de `PUBLIC`, qualquer role entra em qualquer database.
- O PostGIS normalmente exige superuser para `CREATE EXTENSION`, então ele entra no init e não numa migração do Flyway. O `pg_trgm` é uma extensão "trusted".
- Por enquanto, crie só o database do Imóveis, mas organize o script para que adicionar outro serviço seja repetir um bloco.

**Documentação**

- `ALTER DEFAULT PRIVILEGES`: https://www.postgresql.org/docs/current/sql-alterdefaultprivileges.html
- Schemas e o schema `public`: https://www.postgresql.org/docs/current/ddl-schemas.html
- `CREATE EXTENSION`: https://www.postgresql.org/docs/current/sql-createextension.html

---

## T3. Kafka em KRaft, nó único

**Critérios de aceite**

- [ ] A imagem tem tag fixa.
- [ ] O broker fica `healthy`.
- [ ] Um tópico de teste é criado, e você produz e consome nele de dentro da rede do compose.
- [ ] Um consumidor **com group id** funciona.
- [ ] Do host, `kcat -b localhost:<porta> -L` mostra o endereço anunciado acessível a partir do host. O `kcat` é opcional, mas ajuda bastante.

**Pontos de atenção**

- A maior armadilha são os listeners:
  - um interno, para os containers;
  - um externo, para o host e a IDE;
  - um do controller;
  - e os *advertised listeners* de cada um.
- Num nó único, os tópicos internos precisam de fator de replicação 1 (`offsets.topic.replication.factor` e os de transação). Sem isso, consumer groups falham com "coordinator not available".
- O modo KRaft exige um `CLUSTER_ID`.

**Documentação**

- Kafka (KRaft e listeners): https://kafka.apache.org/documentation/
- Variáveis das imagens da Confluent: https://docs.confluent.io/platform/current/installation/docker/config-reference.html
- kcat: https://github.com/edenhill/kcat

---

## T4. Confluent Schema Registry

**Critérios de aceite**

- [ ] A imagem tem tag fixa, compatível com a versão do Kafka.
- [ ] `curl localhost:<porta>/subjects` devolve `[]`.
- [ ] Você registra um schema de teste pela API REST, lê de volta e depois apaga.
- [ ] `GET /config` mostra o nível de compatibilidade. O padrão é `BACKWARD`; o modo definitivo é decisão da etapa 2.
- [ ] O tópico `_schemas` aparece no Kafka.

**Pontos de atenção**

- O registry aponta para o listener **interno** do Kafka.

**Documentação**

- API REST do Schema Registry: https://docs.confluent.io/platform/current/schema-registry/develop/api.html

---

## T5. Kafka UI

**Critérios de aceite**

- [ ] A interface mostra o cluster, os tópicos, os consumer groups e os subjects do Schema Registry.

**Pontos de atenção**

- O projeto `provectuslabs/kafka-ui` parou de ser mantido; o fork ativo é o `kafbat/kafka-ui`.
- Use `depends_on` com `condition: service_healthy`.

**Documentação**

- kafbat/kafka-ui: https://github.com/kafbat/kafka-ui
- `depends_on` no Compose: https://docs.docker.com/reference/compose-file/services/#depends_on

---

## T6. Ordem de subida, reprodutibilidade e documentação

**Critérios de aceite**

- [ ] Todo serviço tem healthcheck, e as dependências usam `service_healthy`.
- [ ] A definição de pronto, no início deste arquivo, funciona.
- [ ] Existe um `docs/infra.md` com URLs, portas e credenciais de desenvolvimento.
- [ ] O status da etapa está atualizado no `CLAUDE.md`.
- [ ] As regras de negócio e as decisões em aberto da etapa 2 foram revisadas, e as que bloqueiam o início dela estão decididas.
