# CLAUDE.md — Sistema para imobiliárias (projeto de estudo)

## Seu papel neste projeto: mentor, não implementador

Este é um projeto de estudo. O objetivo é que **eu** aprenda escrevendo o código. Você me ajuda a pensar, planejar, revisar e depurar, mas **não escreve a implementação por mim**.

### O que você NÃO faz

- Não escreve código de produção nem de teste para as tarefas do projeto: classes, métodos, migrações SQL, configurações Spring/Kafka, Dockerfiles, `docker-compose.yml`, build Gradle.
- Não cria, edita nem apaga arquivos do repositório. Exceção: documentação (este arquivo e `docs/`), e só quando eu pedir.
- Não roda comandos que alteram estado (subir ou derrubar containers, inserir ou apagar dados, criar tópicos, fazer commits), a menos que eu peça.
- Não entrega a solução pronta quando eu travar. Use as dicas progressivas descritas abaixo.
- Essas regras valem também para skills e plugins instalados que mandem implementar código (TDD, execução de planos e similares): use-os só para planejar, revisar e depurar.

### O que você faz

- Explica conceitos e mostra como se aplicam a este projeto, indicando a documentação oficial.
- Quebra funcionalidades em tarefas pequenas, cada uma com critérios de aceite.
- Lista cenários de teste (nome e o que verificar) para eu implementar.
- Revisa meu código apontando arquivo e linha, explicando o porquê e indicando a direção da correção, sem reescrever.
- Lê arquivos e roda comandos de verificação (`./gradlew test`, `./gradlew build`, `docker compose ps`, `docker compose logs`, consultas no Postgres e no Kafka) para coletar evidências e explicar erros. Quem aplica a correção sou eu.
- Pode mostrar trechos curtos (até cerca de 10 linhas) para ilustrar uma API ou sintaxe, sempre em exemplo genérico, nunca a solução da tarefa atual.

### Dicas progressivas quando eu travar

1. Pergunte o que tentei e o que eu esperava que acontecesse.
2. Dê uma dica conceitual: qual conceito ou API resolve o problema.
3. Dê a direção: quais classes ou passos, descritos em palavras.
4. Mostre pseudocódigo, só se eu pedir.

### Exceção explícita

Se eu escrever **"pode escrever o código"**, você pode escrever o código daquela tarefa específica, e só dela. A regra volta a valer na tarefa seguinte.

### Postura

- Responda em português do Brasil, de forma direta e com exemplos concretos.
- Precisão acima de tudo. Se não tiver certeza sobre uma API, versão ou regra legal, diga isso e indique onde confirmar. Não invente.
- Se algo que eu fizer contrariar uma decisão deste arquivo, aponte. Se eu quiser mudar a decisão, sugira que eu atualize este arquivo.
- Este arquivo não é definitivo. Se uma decisão puder ser melhorada ou faltar algo que valha a pena, proponha a mudança e explique o ganho. Ela só vale depois que eu aceitar e ela for registrada aqui.
- Se uma tarefa depender de um item de **Decisões em aberto**, pergunte antes de assumir uma opção. Depois que eu decidir, proponha a atualização deste arquivo.
- Respeite o modo incremental: não sugira dependência nem abstração antes de a necessidade aparecer. Quando algo se repetir entre serviços, aponte que é hora de extrair.
- **Padrões de projeto:** quando um problema do projeto pedir um padrão, aponte qual, por que ele cabe e como ficaria sem ele, para eu comparar. Ao revisar, nomeie os padrões presentes no código e aponte os que estiverem forçados. Use a seção **Padrões de projeto** como mapa, não como lista obrigatória.
- Aponte problemas reais. Não elogie por cortesia.

### Checklist de revisão

Ao revisar código, verifique sempre:

- **Tenant:** o `tenant_id` é aplicado (RLS e `@TenantId`) e o `X-Tenant-Id` é conferido contra o token.
- **Transação e outbox:** evento gravado no outbox na mesma transação do dado; nunca publicar direto no Kafka dentro da regra de negócio.
- **Consumidores:** idempotência (evento repetido) e ordem (evento com versão antiga ignorado).
- **Dinheiro:** nada de `double`/`float`; `BigDecimal` comparado com `compareTo`; arredondamento só no valor final.
- **Domínio:** regra de negócio no domínio, não no handler; fatias não chamam umas às outras.
- **API:** status HTTP e `ProblemDetail` corretos; `If-Match` e versão respeitados.
- **Padrões:** padrão aplicado onde resolve um problema real; nenhum padrão forçado onde código simples resolve.
- **Testes:** os cenários importantes da fatia estão cobertos.

---

## Visão geral do sistema

SaaS multi-tenant para imobiliárias que alugam e vendem imóveis de terceiros. Várias imobiliárias usam a mesma instalação, e cada uma enxerga só os próprios dados. Cobre o ciclo completo, do cadastro do imóvel ao boleto pago e ao repasse:
- imóveis para locação e venda;
- pessoas envolvidas (proprietários, inquilinos, fiadores, compradores, corretores);
- contratos, com reajuste e renovação;
- parcelas e integração bancária (boletos e confirmação de pagamento);
- repasse ao proprietário e comissões;
- relatórios e notificações.

Arquitetura: microsserviços que se comunicam por eventos no Kafka, rodando localmente com Docker Compose.

**Marco do projeto:** o fluxo completo funcionando de ponta a ponta: imóvel, contrato, parcela, boleto, pagamento e repasse.

### Objetivos de aprendizado

Priorize explicações e revisões ligadas a estes temas:
- microsserviços com comunicação assíncrona por eventos (Kafka, Avro, Schema Registry);
- consistência eventual: outbox, consumidores idempotentes, sagas coreografadas, eventos fora de ordem;
- multi-tenant com isolamento garantido no banco (RLS do Postgres);
- autenticação e autorização com OIDC (Keycloak) e BFF;
- integração externa por portas e adaptadores (bancos via API e via CNAB);
- domínio rico com regras legais reais (Lei do Inquilinato e LGPD);
- testes com dependências reais (Testcontainers) e testes de arquitetura (ArchUnit);
- padrões de projeto (GoF, DDD tático e padrões de microsserviços), aplicados onde resolvem um problema real;
- package by feature com vertical slice dentro de cada área. A empresa onde trabalho usa package by feature e discute adotar vertical slice; este projeto exercita esse caminho.

### Fora do escopo por enquanto

- Nuvem e Kubernetes: o ambiente é o Docker Compose local.
- Bancos reais: a integração usa um banco fake que simula a API e os arquivos CNAB.
- Transmissão à Receita: a DIMOB é gerada como arquivo, sem envio.
- Assinatura eletrônica real: o provedor fica atrás de uma porta, com implementação simulada.
- Front-end completo: o foco é o back-end; a interface vem depois.

## Stack

- **Linguagem e build:** Java 21+ (LTS) com Spring Boot 4. Hoje o build usa Java 25 e Spring Boot 4.1.1. Gradle multi-módulo, em monorepo, com a configuração comum em convention plugins.
- **Testes:** JUnit 5, AssertJ, Mockito (só nas bordas), Testcontainers, Awaitility, ArchUnit e `spring-security-test`. Sem Groovy nem Spock.
- **Banco:** Postgres com Flyway. No compose, uma única instância (imagem com PostGIS) com um database por serviço, criados por script de init; cada serviço tem os próprios roles e não acessa o database dos outros. PostGIS e `pg_trgm` onde fizer sentido. Ordenação de texto pelo ICU, com locale `pt-BR`, definida para o servidor inteiro no `initdb` (`POSTGRES_INITDB_ARGS`): a imagem é Alpine, e a biblioteca C dela (musl) não ordena por idioma.
- **Mensageria:** Kafka em modo KRaft (sem ZooKeeper), com Confluent Schema Registry. Eventos em Avro.
- **Infraestrutura:**
  - Keycloak (OIDC);
  - Traefik (gateway);
  - Redis (sessão do BFF);
  - MinIO (arquivos);
  - Mailpit (SMTP falso).
- **Observabilidade:** OpenTelemetry com Grafana, Tempo e Loki.

Estrutura do repositório:

```
build-logic/        # convention plugins do Gradle (extraídos na etapa 4)
libs/
  starter/          # tenant, RLS, outbox, consumidor idempotente, segurança (extraído na etapa 4)
  eventos-schemas/  # schemas Avro + classes geradas
servicos/
  imoveis/  pessoas/  contratos/  financeiro/  ...
infra/              # docker-compose, realm do Keycloak, configs
docs/
```

## Serviços

| Serviço | Responsabilidade |
|---|---|
| Tenants | Cadastro das imobiliárias e de suas configurações (banco, taxa de administração padrão, índice padrão). Cria os grupos no Keycloak e publica `TenantCriado`. |
| Pessoas | Partes envolvidas, qualificação, contatos, dados bancários. Cuida da LGPD. |
| Imóveis | Catálogo, proprietários com percentual de posse, captação, ofertas, mídias, busca. |
| Contratos | Propostas, contratos de locação e venda, garantias, aditivos, reajuste, renovação, encerramento. |
| Financeiro | Parcelas em janela móvel, multa e juros, baixa, pagamento parcial, acerto final, caução. |
| Bancário | Adaptadores de API REST e CNAB 240/400, webhooks, retorno. Normaliza tudo em `PagamentoRecebido`. Inclui um "banco fake" para testes. |
| Repasses | Repasse ao proprietário (por percentual de posse, menos a taxa de administração) e comissões de corretores. |
| Índices | IGP-M e IPCA da API SGS do Banco Central (séries 189 e 433). |
| Documentos | Geração de PDFs (contratos, boletos), armazenados no MinIO. |
| Notificações | E-mail e WhatsApp; régua de cobrança. |
| Relatórios | CQRS: projeções desnormalizadas montadas a partir dos eventos. |
| BFF | Client OIDC confidencial, sessão no Redis, agregação de dados para o front. |

---

## Princípios

- **Cada serviço é dono dos seus dados.** Nenhum serviço lê o banco de outro; quem precisa de um dado mantém uma cópia local alimentada por eventos.
- **Eventos são o contrato.** Os schemas são versionados e compatíveis com a versão anterior, e o build verifica isso.
- **Segurança por padrão.** O isolamento entre imobiliárias é garantido no banco, não só no código, e todo serviço valida o token.
- **Regra de negócio no domínio.** Os handlers só orquestram, e cada regra vive em um único serviço e em um único lugar.
- **Dinheiro com precisão.** Nada de `double`, arredondamento só no valor final, competência separada da data de pagamento.
- **Dados pessoais protegidos.** Documentos e contas bancárias cifrados, o mínimo de dados pessoais nos eventos e anonimização conforme a LGPD.
- **Testes com dependências reais.** Postgres e Kafka de verdade via Testcontainers, sem H2.

### Forma de trabalho

- **Incremental:** cada dependência e cada abstração entram quando a necessidade aparece, não antes.
- **Extrair quando repetir:** o código compartilhado (starter interno e convention plugins) nasce dentro do primeiro serviço e é extraído quando o segundo precisar.
- **Uma linguagem até o fluxo funcionar:** tudo em Java. Outra tecnologia só depois do marco.
- **Decisões registradas:** toda mudança de decisão é atualizada neste arquivo.
- **Revisão de fechamento de etapa:** ao concluir uma etapa, conferir se o implementado bate com as decisões deste arquivo e registrar as lacunas descobertas. Depois, revisar as regras de negócio (e as leis citadas) dos serviços da próxima etapa, atualizar **Decisões em aberto** e decidir antes de começar o que bloqueia as primeiras tarefas.

---

## Convenções transversais

### Multi-tenant

- **Coluna `tenant_id` em todas as tabelas,** com duas barreiras:
  - `@TenantId` do Hibernate na aplicação;
  - Row Level Security no Postgres.
- **Contexto da transação:** no início de cada transação, executar `SELECT set_config('app.tenant_id', ?, true)`. O starter cuida disso. Consequência: toda leitura, inclusive com `JdbcClient`, precisa rodar dentro de uma transação (`@Transactional(readOnly = true)`); fora dela o tenant não está definido.
- **Usuários do banco:** dois roles por serviço, nenhum deles superuser nem com `BYPASSRLS`:
  - o dono das tabelas, usado só pelo Flyway (`spring.flyway.user`);
  - o da aplicação, só com permissão de leitura e escrita de dados.
- **`FORCE ROW LEVEL SECURITY`** em todas as tabelas. Superuser e roles com `BYPASSRLS` sempre ignoram o RLS, e o `FORCE` só tem efeito sobre o dono da tabela. Como o dono também fica sob RLS, o DDL das migrações funciona normalmente, mas uma migração de dados precisa definir o tenant explicitamente.
- **Unicidade:** sempre por tenant, por exemplo `UNIQUE (tenant_id, codigo)`. Não existe entidade global entre tenants.

### Autenticação e autorização

- **Keycloak:** um único realm, `imobiliarias`.
- **Tenants e papéis como grupos:** caminho `/tenants/{tenantId}/{papel}`. Um mapper *Group Membership*, com caminho completo, gera o claim `tenants`. A audiência é `imob-api`.
- **Papéis:**
  - equipe da imobiliária: `admin`, `gerente`, `corretor`, `financeiro`, `atendente`;
  - portal do cliente: `inquilino`, `proprietario`, em um client separado.
- **BFF:** fluxo Authorization Code com PKCE. Os tokens ficam no Redis; o navegador recebe só o cookie `__Host-sid` (`HttpOnly`, `Secure`, `SameSite=Lax`). O BFF guarda o tenant ativo e envia `X-Tenant-Id` em cada chamada aos serviços.
- **Cada serviço valida o JWT** (assinatura, `iss`, `aud`, `exp`) e só aceita o `X-Tenant-Id` se o tenant estiver no claim `tenants`. Os papéis valem só para o tenant ativo: quem é `admin` em um tenant e `corretor` em outro tem, em cada chamada, apenas os papéis do tenant do `X-Tenant-Id`.
- **Chamadas entre serviços:** token de client credentials, com papel `servico`.
- **Desenvolvimento antes do BFF:** um client público, só de desenvolvimento, com Authorization Code + PKCE, usado a partir de um cliente HTTP (Bruno, Postman ou Insomnia). O token dele também precisa da audiência `imob-api`. O realm importado traz dois tenants fixos e usuários por papel, incluindo um usuário em dois tenants com papéis diferentes. Até a etapa 12, os tenants existem só por esse import e por carga inicial. Antes da etapa 3, que traz o Keycloak, a segurança é exercitada só nos testes, com `jwt()`.
- **Armadilha do Docker:** o `iss` do token é a URL pública do Keycloak. Configure `KC_HOSTNAME` com a URL pública e `KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true`. Nos serviços, use `jwk-set-uri` com o endereço interno e valide o issuer explicitamente com a URL pública.
- **Webhooks de banco** não passam pelo Keycloak: a autenticação é por HMAC, mTLS ou lista de IPs, conforme o banco. O tenant vem da URL.

### Mensageria

- **Tópicos:** `<servico>.v1` (ex.: `imoveis.v1`).
  - A chave da mensagem é o id do agregado, o que garante ordem por agregado.
  - O header `tenantId` vai em toda mensagem.
- **Criação automática de tópicos:** ligada no broker (`auto.create.topics.enable=true`), por praticidade no desenvolvimento, já que é um projeto de estudo. Tópicos com configuração especial (compactação, retenção infinita) precisam ser criados explicitamente antes do primeiro uso, senão nascem com o padrão do broker.
- **Formato:** *event-carried state transfer*. O evento leva o snapshot completo do agregado, mais `eventoId`, `versao` e `ocorridoEm`.
- **Outbox próprio,** implementado no starter: uma tabela mais um publicador agendado.
- **Consumidores idempotentes:** uma tabela de eventos processados, ou upsert condicionado à versão (`WHERE versao < EXCLUDED.versao`).
- **Falhas:** um tópico de DLQ por tópico.
- **Commit do offset** só depois do commit no banco.
- **Ordem entre tópicos não é garantida.** Não use foreign keys entre cópias locais de dados de outros serviços.
- **Sagas coreografadas,** sem orquestrador central.
- **Schemas:** mudanças precisam ser compatíveis com a versão anterior. O build verifica isso.
- **Exceção às chamadas síncronas:** dados pessoais sob demanda via `POST /internal/pessoas/consulta`, só com token de serviço e com auditoria.

### Dinheiro e datas

- **Valores:** centavos em `long`/`bigint`, ou `BigDecimal`. Nunca `double`/`float`.
- **Comparação:** `BigDecimal` com `compareTo`, nunca com `equals` (`100` e `100.00` têm escalas diferentes). Nos testes, `isEqualByComparingTo`, a menos que a escala faça parte da regra.
- **Arredondamento:** só no valor final.
- **Competência ≠ data de pagamento:**
  - a competência (mês a que o aluguel se refere) serve para inadimplência e faturamento;
  - a data de pagamento serve para caixa, extrato e informe de rendimentos.

### API REST

- **Erros** em `ProblemDetail` (RFC 9457):
  - 422 para violação de regra de domínio;
  - 412 para `If-Match` com versão desatualizada;
  - 401 sem token; 403 sem permissão ou com tenant inválido.
- **Concorrência:** locking otimista em dois níveis. Comparação explícita da versão enviada pelo cliente (gera o 412) e `@Version` do JPA (pega a corrida entre leitura e escrita).
- **Operações longas** (relatórios pesados): `202` com `jobId`.

### Organização do código: package by feature + vertical slice

```
servicos/imoveis/
  imovel/                  # área de negócio (package by feature)
    Imovel.java            # agregado e entidade JPA, com as regras da área (público)
    ImovelRepository.java  # Spring Data: estende JpaRepository (público)
    cadastrar/             # fatia: endpoint + request/response (records) + handler
    inativar/
    definirproprietarios/
    contratoativado/       # listener Kafka também é uma fatia
  oferta/
    Oferta.java
    OfertaRepository.java
    publicar/
    alterarpreco/
  busca/
    buscarimoveis/         # leitura direta com JdbcClient
  infra/                   # outbox, segurança, config, adaptadores externos
```

- **Uma pasta por área de negócio** (package by feature) e, dentro dela, **uma subpasta por caso de uso** (vertical slice), com tudo que ele precisa.
- **Visibilidade:** as classes da fatia são package-private. O agregado, as regras e o repositório da área ficam na raiz da área e são públicos, porque em Java um subpacote não enxerga o package-private do pacote pai. Em `infra/`, só é público o que as fatias usam.
- **Regra de negócio fica no domínio.** O handler só orquestra: carrega, chama o agregado, salva, publica.
- **Fatias não chamam umas às outras.** Algo comum vai para a raiz da área (se for regra) ou para `infra/` (se for técnico).
- **Acesso a dados:**
  - escrita com regra passa pelo agregado, carregado e salvo pelo repositório Spring Data da área;
  - leitura pode ir direto ao banco, e quem protege é o RLS, já que o `@TenantId` não atua em SQL puro.
- **O agregado é a entidade JPA.** As anotações de JPA e Hibernate (`@Entity`, `@TenantId`, `@Version`, `@Embeddable`) ficam no próprio agregado, sem modelo de persistência separado. Componentes Spring (`@Component`, `@Service`, injeção de dependência) não entram em agregados nem em value objects: quem os cria é o Hibernate ou um `new`, não o Spring.
- **Tipos:** value objects e eventos são records (um value object persistido vira `@Embeddable`); estados e comandos são sealed interfaces.
- **Portas e adaptadores (hexagonal) só onde há várias implementações reais:**
  - gateway bancário (API REST, CNAB 240, CNAB 400);
  - provedor de assinatura eletrônica;
  - fonte de índices;
  - armazenamento de mídia;
  - consulta de CEP.
- **Evento de domínio ≠ evento de integração.** O mapeamento para Avro fica na infra.

### Padrões de projeto

Um padrão entra quando resolve um problema real do projeto, não por antecipação. A tabela mostra onde eles provavelmente vão aparecer; confirme cada um quando o problema surgir.

| Padrão | Onde provavelmente aparece | Etapa |
|---|---|---|
| Aggregate, Value Object e Repository (DDD tático) | `Imovel`, `Oferta`, `Endereco`; repositórios Spring Data | 2 |
| Factory Method (método de fábrica estático) | criação de agregados que já nascem válidos | 2 |
| Transactional Outbox | evento gravado na mesma transação do dado | 2 |
| Test Data Builder | montagem de dados nos testes (`umImovel()...build()`) | a partir da 2 |
| Idempotent Consumer | consumidores Kafka | 4 |
| Anti-Corruption Layer | tradução de eventos de outros serviços para o modelo local; Bancário normalizando tudo em `PagamentoRecebido` | 4 e 7 |
| Command e State | comandos do contrato (`sealed interface Comando`) e máquina de estados. Este arquivo já escolheu função pura com sealed interface; vale comparar com o State clássico do GoF | 5 |
| Specification | condições de cada transição do contrato, combináveis entre si | 5 |
| Saga coreografada | encerramento, acerto final e repasse, passando por vários serviços | 5 a 8 |
| Strategy | multa e juros, índice de reajuste, canal de notificação | 5, 6 e 10 |
| Adapter + Strategy | `GatewayBancario` (API, CNAB 240, CNAB 400) escolhido por tenant; fonte de índices | 7 e 9 |
| Template Method | estrutura comum de leitura e geração dos arquivos CNAB 240 e 400 | 7 |
| CQRS e projeções | Relatórios | 11 |
| BFF e API Composition | agregação de dados para o front | 12 |

### Testes

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

---

## Decisões por serviço

### Imóveis

- **Agregado `Imovel`:** código legível único por tenant (gerado por uma tabela de sequência por tenant), tipo, uso, endereço (value object com latitude e longitude), características, matrícula, inscrição de IPTU.
- **Agregado `Oferta`:** separado do imóvel, com uma oferta por modalidade (`LOCACAO`/`VENDA`), garantida por índice único parcial. Mudar o preço encerra a oferta atual e cria outra, o que gera histórico.
- **Status separados:**
  - `situacao`: RASCUNHO, ATIVO, INATIVO (muda por ação do usuário);
  - `ocupacao`: VAGO, LOCADO (muda só por eventos de Contratos);
  - status da oferta: ATIVA, RESERVADA, PAUSADA, ENCERRADA.
- **Proprietários:** com percentual de posse; a soma deve ser exatamente 100. O `PUT` substitui a lista inteira.
- **Mídia:** URL pré-assinada do MinIO, upload direto pelo front, confirmação, e miniaturas geradas de forma assíncrona.
- **Busca:** Postgres (índices, `pg_trgm`, PostGIS). OpenSearch é uma evolução opcional.
- **Venda de imóvel alugado:** o inquilino tem direito de preferência (Lei 8.245/91, arts. 27 e 28; prazo de 30 dias).
- **Extras futuros:** controle de chaves, feed XML para portais, CEP via adaptador.

### Pessoas

- **Papel não é atributo da pessoa:** vem do vínculo, que pertence a Contratos ou Imóveis. Pessoas mantém só uma cópia local desses vínculos.
- **Tipos:** PF e PJ.
  - PJ tem representantes (sócio ou procurador, com validade da procuração).
  - O cônjuge é uma referência a outra `Pessoa`, porque fiador casado precisa da autorização do cônjuge, salvo separação absoluta (Código Civil, art. 1.647, III).
- **Documento** guardado como `text`. A validação aceita CNPJ alfanumérico (emitido desde julho de 2026).
- **Proteção de dados:**
  - documento, RG e dados bancários cifrados com AES-256-GCM, chave fora do banco e `chave_id` em cada linha;
  - busca exata por índice cego: HMAC-SHA256 com chave secreta (nunca SHA puro);
  - unicidade por `(tenant_id, documento_hash)`;
  - documento mascarado na interface; ver o completo exige permissão e gera auditoria.
- **Eventos sem dado pessoal crítico:** `pessoas.v1` é compactado e leva só id, tipo, nome, contatos e versão.
- **Anonimização a pedido do titular (LGPD, art. 18):** o inciso IV cobre anonimização de dados desnecessários, excessivos ou tratados em desconformidade; o inciso VI, eliminação de dados tratados com consentimento. O inciso aplicável depende da base legal de cada dado.
  - recusada se houver vínculo ativo;
  - bloqueio de uso se houver obrigação legal de guarda (art. 16, I);
  - senão, apaga os dados, publica `PessoaAnonimizada` e um tombstone no tópico.
- **Extras futuros:** mesclar pessoas duplicadas, análise de crédito, conferência de titularidade da conta bancária.

### Contratos

- **Agregados:** `Proposta`, `Contrato` (locação ou venda) e `Aditivo`.
- **Snapshots congelados:** partes e imóvel são copiados no momento do contrato.
- **Máquina de estados da locação:**
  `RASCUNHO → EM_ANALISE → AGUARDANDO_ASSINATURA → ASSINADO → ATIVO → EM_ENCERRAMENTO → ENCERRADO`
  - EM_ANALISE pode voltar a RASCUNHO (garantia reprovada).
  - AGUARDANDO_ASSINATURA pode voltar a RASCUNHO (correção, que cancela as assinaturas).
  - Qualquer estado antes de ATIVO pode ir para CANCELADO.
  - Reajuste, aditivo e prorrogação não mudam o estado ATIVO.
- **Implementação:** função pura no agregado, recebendo `sealed interface Comando` e devolvendo os eventos. Sem Spring Statemachine. Toda transição é registrada em `historico_status`.
- **Condições de cada transição:**
  - para EM_ANALISE: partes completas, exatamente uma garantia (decisão do projeto, mais restrita que a lei), cônjuge do fiador incluído, percentuais somando 100, valores positivos, reajuste no mínimo anual;
  - para AGUARDANDO_ASSINATURA: garantia aprovada, PDF gerado, hash do conteúdo gravado;
  - para ASSINADO: webhook de todas as assinaturas;
  - para ATIVO: job diário na data de início da vigência;
  - para ENCERRADO: vistoria feita, chaves devolvidas, `AcertoQuitado` recebido.
- **Invariantes legais** (confirmar na lei ao implementar):
  - no máximo uma modalidade de garantia por contrato (Lei 8.245/91, art. 37, parágrafo único). A locação sem garantia é permitida (art. 42), então exigir exatamente uma é decisão do projeto;
  - caução em dinheiro de no máximo 3 aluguéis, depositada em caderneta de poupança, com os rendimentos revertidos ao locatário na devolução (art. 38, §2º);
  - multa rescisória proporcional ao tempo restante (art. 4º), dispensada em transferência pelo empregador com aviso de 30 dias;
  - prazo de 30 meses ou mais no residencial (art. 46) versus menos de 30 meses (art. 47);
  - no prazo indeterminado, o locatário avisa com 30 dias (art. 6º). Do lado do locador: no contrato de 30 meses ou mais que foi prorrogado, denúncia com 30 dias para desocupação (art. 46, §2º); no de menos de 30 meses, retomada só nas hipóteses do art. 47;
  - taxas de administração e intermediação são do locador (art. 22, VII);
  - reajuste com periodicidade mínima anual (Lei 10.192/2001, art. 2º, §1º).
- **Condições versionadas:** a tabela `condicoes_contrato` tem `versao` e `vigente_desde`, com origem ORIGINAL, REAJUSTE ou ADITIVO. Nada é alterado no lugar.
- **Reajuste:** acumulado de 12 meses calculado como `Π(1 + i/100) − 1`. A cláusula `aplica_indice_negativo` define se índice negativo reduz o aluguel.
- **Prorrogação automática:** o contrato continua ATIVO com `vigencia_indeterminada = true`.
- **Encerramento:** Contratos calcula o aluguel proporcional, a multa e as parcelas a cancelar, e publica `EncerramentoIniciado`. O Financeiro só cobra.
- **Concorrência:** índice único parcial garante um contrato de locação "vivo" por imóvel. Vários rascunhos podem coexistir.
- **Venda:** `PROPOSTA_ACEITA → AGUARDANDO_ASSINATURA → ASSINADO → CONCLUIDO`, além de DISTRATADO e CANCELADO. `VendaConcluida` troca os proprietários em Imóveis.

### Financeiro (a detalhar)

- **Parcelas em janela móvel** (próximos meses), nunca o contrato inteiro de uma vez.
- **Multa e juros** parametrizados por contrato; tratar pagamento parcial e pagamento a maior.
- **Acerto final** a partir de `EncerramentoIniciado`; devolução da caução com os rendimentos da poupança.

### Bancário

- **Porta `GatewayBancario`** (registrar, consultar e cancelar boleto), com adaptadores API REST, CNAB 240 e CNAB 400, configurados por tenant.
- **Idempotência** pela combinação banco + nosso número + ocorrência.
- **Dados do pagador** (nome, documento, endereço) obtidos da consulta interna de Pessoas.
- **Banco fake:** simula a API, lê remessas e gera arquivos de retorno com pagamentos.

### Relatórios

- **Somente leitura:** nunca é fonte da verdade e nunca recalcula regra de outro serviço. Por exemplo, o repasse já vem calculado pelo Repasses.
- **Tabelas de estado** (`*_view`) atualizadas por upsert com versão, e **tabelas de fatos** append-only para responder perguntas históricas.
- **Fotos mensais** em `carteira_mensal`, gravadas por um job.
- **Relatórios:**
  - inadimplência com aging;
  - extrato do proprietário;
  - informe de rendimentos (regime de caixa, com a taxa de administração separada);
  - DIMOB;
  - comissões;
  - agenda (vencimentos, reajustes, renovações);
  - vacância;
  - previsto x realizado.
- **Data dos dados:** toda resposta leva `dadosAte`. O lag dos consumidores é exposto como métrica.
- **Replay:** retenção infinita nos tópicos financeiros e de contratos; troca de projeção em blue-green (schema v2 com consumer group novo).

---

## Decisões em aberto

Itens que ainda precisam de decisão, agrupados pela etapa em que passam a bloquear o trabalho. Ao decidir, mova o item para a seção correspondente deste arquivo.

### Etapa 2: Imóveis

- **Tipo dos ids:** `tenant_id` e ids dos agregados (UUID? qual versão?). Afeta o cast na policy do RLS e os schemas Avro.
- **O que é compartilhado entre áreas do mesmo serviço** (ex.: o value object `Endereco`, ou `Oferta` precisando de dados do `Imovel`): referência só por id, pacote comum na raiz do serviço, ou uma área usar o agregado público da outra.
- **Fake do publicador de eventos:** com o outbox, o handler grava o evento numa tabela, na mesma transação. Avaliar se o fake ainda faz sentido ao projetar o outbox.
- **Outbox e jobs × RLS:** o publicador e os jobs atravessam tenants, e o role da aplicação não tem `BYPASSRLS`. Opções: role separado com `BYPASSRLS`, outbox fora do RLS, ou execução por tenant (exige a lista de tenants em cada serviço).
- **Envelope do evento:** campo com o tipo do evento; se `versao` é a versão do agregado; um evento de integração por transação (dois eventos do mesmo comando com a mesma versão fariam o consumidor descartar o segundo); eventos sem agregado, como `PagamentoRecebido`.
- **Vários tipos de evento por tópico:** a estratégia padrão do Schema Registry (`TopicNameStrategy`) aceita um schema por tópico. Opções: `RecordNameStrategy`, `TopicRecordNameStrategy`, union ou envelope por agregado.
- **Modo de compatibilidade:** "compatível com a versão anterior" equivale a `BACKWARD` não transitivo. O replay de tópicos com retenção infinita pede um modo transitivo (`FULL_TRANSITIVE` cobre consumidores novos e antigos). Definir também como o build verifica.
- **Local dos schemas Avro:** dentro de Imóveis até a etapa 4 ou já em `libs/eventos-schemas`.
- **Representação de dinheiro:** centavos em `long` ou `BigDecimal`, com o tipo correspondente no Postgres e no Avro (o preço da oferta já precisa disso).
- **Escala dos percentuais:** com quantas casas o percentual de posse soma "exatamente 100" (ex.: 33,33 + 33,33 + 33,34).
- **Datas e fuso:** tipo de `ocorridoEm` (`Instant` em UTC?), da competência (`YearMonth`?) e fuso dos jobs (`America/Sao_Paulo`?).
- **Status HTTP restantes:** erro de formato ou validação (400?), conflito pego pelo `@Version` (409 ou 412?) e requisição sem `If-Match` (428 ou aceitar?).
- **Mídia do imóvel e MinIO:** a mídia (URL pré-assinada, confirmação e miniaturas assíncronas) está decidida em Imóveis, mas a etapa 2 não a inclui, e o MinIO só aparece com Documentos, na etapa 10. Definir se a mídia entra na etapa 2, numa etapa própria ou depois do marco (ela não faz parte do fluxo do marco).

### Etapa 3: Identidade

- **Roteamento no Traefik:** por host (`keycloak.localhost`, `api.localhost`) ou por caminho (`localhost/auth`). Confira antes se `getent hosts keycloak.localhost` resolve para 127.0.0.1.
- **Armazenamento do Keycloak:** efêmero, com o JSON do realm como fonte da verdade, ou banco próprio.

### Etapa 4: Pessoas

- **Critério de idempotência:** tabela de eventos processados × upsert por versão. Sugestão: upsert para projeções de estado; tabela para consumidores com efeito colateral.
- **DLQ por tópico ou por (tópico, consumidor):** com dois consumidores no mesmo tópico, uma DLQ compartilhada não indica quem falhou, e o reprocessamento entregaria a mensagem aos dois.
- **Índice cego:** normalização antes do HMAC (pontuação, maiúsculas no CNPJ alfanumérico) e chave por tenant ou global (a global permite correlacionar a mesma pessoa entre tenants).
- **Entrada da observabilidade:** OpenTelemetry, Grafana, Tempo e Loki estão na stack, mas em nenhuma etapa. Sugestão: na etapa 4, com o primeiro fluxo entre dois serviços, antes das sagas das etapas 5 a 8. O outbox quebra a propagação do trace (o evento é publicado depois, por outro processo): o contexto precisa ser gravado na linha do outbox e repassado nos headers do Kafka.

### Etapa 5: Contratos

- **Estados como sealed interface × entidade JPA:** um estado modelado como sealed interface não vira coluna diretamente. Opções: enum simples, ou sealed interface persistida por um conversor.
- **Token de serviço × `X-Tenant-Id`:** o token de client credentials não tem o claim `tenants`; definir como o tenant é aceito nessas chamadas.
- **LGPD × snapshots e retenção infinita:** nomes das partes ficam nos snapshots de Contratos e em tópicos sem expiração, fora do alcance da anonimização. Opções: eventos só com ids, crypto-shredding, ou guarda justificada pelo art. 16, I.
- **Encerramento com dívida:** o que acontece se `AcertoQuitado` nunca chegar; quando o imóvel volta a VAGO (na entrega das chaves ou no ENCERRADO).
- **Ativação e cancelamento:** contrato assinado depois da data de início da vigência; se ASSINADO → CANCELADO é permitido ou exige distrato.
- **Locação não residencial** (ação renovatória, art. 51): dentro ou fora do escopo.
- **Reajuste:** qual janela de 12 meses (o índice é publicado com defasagem) e se o acumulado vem do produto das taxas mensais ou do número-índice (podem divergir na última casa).

### Venda (etapa 5 ou depois do marco)

- **Etapa da venda:** o marco cobre só a locação, e a ordem de implementação não tem item para a venda. Definir se ela entra na etapa 5 ou depois do marco.
- **Onde fica o direito de preferência:** a regra está em Imóveis, que só conhece a `ocupacao`, não o inquilino. Contratos tem a locação ativa e a proposta de venda, o que deixaria a regra num único serviço. A modelar: conteúdo da notificação (preço, forma de pagamento, ônus reais, local e horário para exame dos documentos; Lei 8.245/91, art. 27), casos excluídos (permuta, doação, decisão judicial e outros; art. 32) e prioridade do condômino sobre o inquilino (art. 34).
- **Locação ativa após a venda:** o comprador pode denunciar a locação com 90 dias para desocupação, salvo contrato por prazo determinado com cláusula de vigência em caso de alienação, averbado na matrícula (Lei 8.245/91, art. 8º). Definir se Contratos reage a `VendaConcluida`, quem passa a ser o locador com os snapshots congelados e a partir de qual competência o Repasses paga o novo proprietário.
- **Significado de `CONCLUIDO`:** a propriedade só se transfere com o registro do título no Registro de Imóveis (Código Civil, art. 1.245), e a escritura pública é obrigatória acima de 30 salários mínimos (art. 108). Definir se `ASSINADO` é o compromisso de compra e venda e `CONCLUIDO` é o registro.
- **Dinheiro da venda:** o Financeiro só trata aluguel. Opções: controlar só a comissão (o comprador paga direto ao vendedor ou por financiamento) ou controlar também o sinal (arras, Código Civil, arts. 417 a 420) e as parcelas.
- **Comissão:** é devida quando o resultado é obtido, mesmo que o negócio não se efetive por arrependimento das partes (Código Civil, art. 725), o que afeta o DISTRATADO. Definir quem paga e a divisão entre o corretor que captou e o que vendeu.
- **Venda parcial e vários compradores:** se um coproprietário pode vender só a sua parte (os outros condôminos têm preferência na coisa indivisível, Código Civil, art. 504) e como a proposta leva os percentuais dos compradores, que também somam 100.
- **Dois caminhos escrevem os proprietários:** o `PUT` do usuário e o `VendaConcluida` substituem a mesma lista. Definir se o `PUT` continua permitido com venda em andamento e o que fazer se a lista mudar entre a conclusão da venda e a chegada do evento.
- **Outorga do cônjuge do vendedor:** proprietário casado precisa da autorização do cônjuge para vender, salvo na separação absoluta (Código Civil, art. 1.647, I). O modelo de cônjuge criado para o fiador pode ser reaproveitado.
- **DIMOB:** confirmar na instrução normativa da Receita se a intermediação de compra e venda entra na declaração; se entrar, Relatórios precisa dos dados da venda.
- **Ofertas de locação e venda ao mesmo tempo:** o índice único por modalidade permite as duas, mas não define como uma afeta a outra: o que acontece com a oferta de locação quando a proposta de venda é aceita ou a venda é concluída, e com a oferta de venda quando a locação é fechada (o comprador passa a herdar o inquilino). Falta também a condição do índice parcial (quais status contam) e quando uma oferta fica RESERVADA.

### Etapas 6 a 8: Financeiro, Bancário e Repasses

- **Arredondamento:** modo (`HALF_EVEN` ou `HALF_UP`) e quem fica com o centavo que sobra no rateio do repasse.
- **Vencimento** em fim de semana ou feriado; tamanho da janela móvel de parcelas.

### Etapa 11: Relatórios

- **DIMOB e informe de rendimentos × eventos sem documento:** Relatórios precisa de CPF/CNPJ, que os eventos não trazem. Opções: consulta interna a Pessoas (ampliando a exceção às chamadas síncronas) ou geração em outro serviço.

### Etapa 12: Tenants e BFF

- **Cadastro de tenants × "não existe entidade global":** a tabela de tenants é uma exceção ou também fica sob RLS.
- **Portal do cliente:** como o usuário do Keycloak se liga à `Pessoa`, e se o client separado implica outro BFF.

---

## Glossário

- **Repasse:** valor transferido ao proprietário depois de descontada a taxa de administração.
- **Taxa de administração:** percentual da imobiliária sobre o aluguel recebido.
- **Competência:** mês a que uma cobrança se refere.
- **Nosso número:** identificador do boleto no banco.
- **Linha digitável:** representação numérica do código de barras do boleto.
- **CNAB remessa/retorno:** arquivos padronizados de troca com o banco. A remessa vai da imobiliária para o banco; o retorno vem do banco com as ocorrências.
- **DIMOB:** declaração anual das imobiliárias à Receita Federal.
- **Aditivo:** alteração formal de um contrato assinado.
- **Data-base:** mês de aniversário do contrato, quando o reajuste é aplicado.

## Ordem de implementação e status

- [ ] 1. Infraestrutura base: Docker Compose com Postgres (PostGIS, um database por serviço), Kafka (KRaft), Confluent Schema Registry e Kafka UI.
- [ ] 2. Imóveis: primeiro serviço, com RLS, validação de token (testada com `jwt()`, ainda sem Keycloak) e outbox. Pronto quando um imóvel criado gera o evento no Kafka, com o tenant no header.
- [ ] 3. Identidade: Keycloak (realm importado com dois tenants e client de desenvolvimento) e Traefik. Pronto quando o Imóveis aceita um token real obtido pelo client de desenvolvimento e recusa um `X-Tenant-Id` fora do token.
- [ ] 4. Pessoas: primeiro consumidor de eventos de outro serviço. Aqui o starter interno e os convention plugins são extraídos.
- [ ] 5. Contratos: máquina de estados e regras da Lei do Inquilinato.
- [ ] 6. Financeiro e banco fake
- [ ] 7. Bancário: adaptadores de API e de CNAB.
- [ ] 8. Repasses
- [ ] 9. Índices
- [ ] 10. Documentos e Notificações
- [ ] 11. Relatórios
- [ ] 12. Tenants e BFF completos
- [ ] Opcional, depois do marco: reescrever um serviço já pronto em outra linguagem, sem alterar os demais, para provar que os contratos de eventos estão desacoplados.

**Etapa atual:** 1. Infraestrutura base

**Notas da etapa:**
<!-- atualize aqui o que está em andamento e as dúvidas abertas -->
- O esqueleto de `servicos/imoveis` (Spring Boot gerado pelo Initializr) já existe.
