# Infraestrutura local

Ambiente de desenvolvimento em Docker Compose. Tudo fica em `infra/`, e os comandos abaixo rodam de dentro dessa pasta.

## Subir pela primeira vez

```
cp .env-exemplo .env
docker compose up -d
docker compose ps
```

- O `.env` não é versionado. Copiá-lo do `.env-exemplo` é o único passo manual depois de clonar o repositório.
- Espere todos os serviços ficarem `healthy` no `docker compose ps`. O Schema Registry e o Kafka UI só sobem depois que o Kafka fica `healthy`.

## Resetar

```
docker compose down -v && docker compose up -d
```

O `-v` apaga os volumes: dados do Postgres, tópicos, offsets dos consumer groups e schemas do Registry. É necessário para:

- rodar de novo os scripts de init do Postgres, que só rodam com o volume vazio;
- trocar o `CLUSTER_ID` do Kafka, que fica gravado no volume.

## Serviços

| Serviço | No host | Na rede do compose | Observação |
|---|---|---|---|
| Postgres | `localhost:5432` | `postgres:5432` | PostGIS, ordenação ICU `pt-BR` |
| Kafka | `localhost:9092` | `kafka:9093` | O listener do controller (9094) não é publicado |
| Schema Registry | http://localhost:8081 | http://schema-registry:8081 | |
| Kafka UI | http://localhost:8082 | `kafka-ui:8080` | Sem login |

- Todas as portas são publicadas só em `127.0.0.1`, então não ficam acessíveis pela rede local.
- A porta 8080 do host fica livre para os serviços Spring Boot.
- Dentro da rede do compose (de um container para outro), use sempre o endereço da segunda coluna. O Kafka anuncia `localhost:9092` só para quem está no host.

## Postgres: databases e usuários

| Usuário | Senha no `.env` | Uso |
|---|---|---|
| valor de `DB_USER` | `DB_PASSWORD` | Superuser, só para administração. Ignora o RLS: nunca usar nos serviços nem nos testes. |
| `imoveis_dono` | `DB_PASSWORD_IMOVEIS_DONO` | Dono do database `imoveis`. Usado só pelo Flyway. |
| `imoveis_app` | `DB_PASSWORD_IMOVEIS_APP` | Aplicação: lê e escreve dados, não cria tabelas. |

- As credenciais de desenvolvimento estão no `.env-exemplo`. Não copie as senhas para outros arquivos.
- Só os roles do serviço conectam no database dele: o `CONNECT` foi revogado de `PUBLIC`.
- Novo serviço: um script `NN-<servico>.sql` em `infra/postgres/docker-entrypoint-initdb.d/`, as senhas novas no `.env-exemplo` e no `environment` do `postgres`, e um reset para o script rodar.

## Comandos úteis

| Para | Comando |
|---|---|
| Abrir o `psql` | `docker compose exec postgres psql -U <DB_USER> -d imoveis` |
| Listar tópicos | `docker compose exec kafka kafka-topics --bootstrap-server kafka:9093 --list` |
| Ver offsets de um group | `docker compose exec kafka kafka-consumer-groups --bootstrap-server kafka:9093 --describe --group <group>` |
| Listar subjects | `curl localhost:8081/subjects` |
| Ver logs de um serviço | `docker compose logs -f <servico>` |

## Armadilhas

- **Criação automática de tópicos ligada:** um nome digitado errado cria um tópico novo em silêncio. Tópicos com configuração especial (compactação, retenção infinita) precisam ser criados antes do primeiro uso.
- **Scripts de init:** mudar um script em `docker-entrypoint-initdb.d` não tem efeito num volume que já existe. É preciso resetar.
