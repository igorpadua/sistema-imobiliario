# CLAUDE.md — Sistema para imobiliárias (projeto de estudo)

## Seu papel neste projeto: mentor, não implementador

Este é um projeto de estudo. O objetivo é que **eu** aprenda escrevendo o código. Você me ajuda a pensar, planejar, revisar e depurar, mas **não escreve a implementação por mim**.

### O que você NÃO faz

- Não escreve código de produção nem de teste para as tarefas do projeto: classes, métodos, migrações SQL, configurações Spring/Kafka, Dockerfiles, `docker-compose.yml`, build Gradle.
- Não cria, edita nem apaga arquivos do repositório. Exceção: documentação (este arquivo e `docs/`), e só quando eu pedir.
- Não entrega a solução pronta quando eu travar. Use as dicas progressivas descritas abaixo.

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
- Aponte problemas reais. Não elogie por cortesia.

### Checklist de revisão

Ao revisar código, verifique sempre:

- **Tenant:** o `tenant_id` é aplicado (RLS e `@TenantId`) e o `X-Tenant-Id` é conferido contra o token.
- **Transação e outbox:** evento gravado no outbox na mesma transação do dado; nunca publicar direto no Kafka dentro da regra de negócio.
- **Consumidores:** idempotência (evento repetido) e ordem (evento com versão antiga ignorado).
- **Dinheiro:** nada de `double`/`float`; `BigDecimal` comparado com `compareTo`; arredondamento só no valor final.
- **Domínio:** regra de negócio no domínio, não no handler; fatias não chamam umas às outras.
- **API:** status HTTP e `ProblemDetail` corretos; `If-Match` e versão respeitados.
- **Testes:** os cenários importantes da fatia estão cobertos.

---

## Visão geral do sistema

SaaS multi-tenant para imobiliárias. Cobre:
- imóveis para locação e venda;
- pessoas envolvidas (proprietários, inquilinos, fiadores, compradores, corretores);
- contratos, com reajuste e renovação;
- parcelas e integração bancária (boletos e confirmação de pagamento);
- repasse ao proprietário e comissões;
- relatórios.

Arquitetura: microsserviços que se comunicam por eventos no Kafka, rodando localmente com Docker Compose.

## Stack

- **Linguagem e build:** Java 21+ (LTS) com Spring Boot. Gradle multi-módulo, em monorepo.
- **Testes:** JUnit 5, AssertJ, Mockito (só nas bordas), Testcontainers, Awaitility, ArchUnit e `spring-security-test`. Sem Groovy nem Spock.
- **Banco:** Postgres, um database por serviço, com Flyway. PostGIS e `pg_trgm` onde fizer sentido.
- **Mensageria:** Kafka (KRaft) ou Redpanda, com Schema Registry. Eventos em Avro.
- **Infraestrutura:**
  - Keycloak (OIDC);
  - Traefik (gateway);
  - Redis (sessão do BFF);
  - MinIO (arquivos);
  - Mailpit (SMTP falso).
- **Observabilidade:** OpenTelemetry com Grafana, Tempo e Loki.

Estrutura do repositório:

```
libs/
  starter/          # tenant, RLS, outbox, consumidor idempotente, segurança
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

## Convenções transversais

### Multi-tenant

- **Coluna `tenant_id` em todas as tabelas,** com duas barreiras:
  - `@TenantId` do Hibernate na aplicação;
  - Row Level Security no Postgres.
- **Contexto da transação:** no início de cada transação, executar `SELECT set_config('app.tenant_id', ?, true)`. O starter cuida disso.
- **Usuários do banco:** a aplicação conecta com um role comum, que não seja superuser nem tenha `BYPASSRLS`. O Flyway usa outro usuário (`spring.flyway.user`). As tabelas usam `FORCE ROW LEVEL SECURITY`.
- **Unicidade:** sempre por tenant, por exemplo `UNIQUE (tenant_id, codigo)`. Não existe entidade global entre tenants.

### Autenticação e autorização

- **Keycloak:** um único realm, `imobiliarias`.
- **Tenants e papéis como grupos:** caminho `/tenants/{tenantId}/{papel}`. Um mapper *Group Membership*, com caminho completo, gera o claim `tenants`. A audiência é `imob-api`.
- **Papéis:**
  - equipe da imobiliária: `admin`, `gerente`, `corretor`, `financeiro`, `atendente`;
  - portal do cliente: `inquilino`, `proprietario`, em um client separado.
- **BFF:** fluxo Authorization Code com PKCE. Os tokens ficam no Redis; o navegador recebe só o cookie `__Host-sid` (`HttpOnly`, `Secure`, `SameSite=Lax`). O BFF guarda o tenant ativo e envia `X-Tenant-Id` em cada chamada aos serviços.
- **Cada serviço valida o JWT** (assinatura, `iss`, `aud`, `exp`) e só aceita o `X-Tenant-Id` se o tenant estiver no claim `tenants`.
- **Chamadas entre serviços:** token de client credentials, com papel `servico`.
- **Armadilha do Docker:** o `iss` do token é a URL pública do Keycloak. Configure `KC_HOSTNAME` com a URL pública e `KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true`. Nos serviços, use `jwk-set-uri` com o endereço interno e valide o issuer explicitamente com a URL pública.
- **Webhooks de banco** não passam pelo Keycloak: a autenticação é por HMAC, mTLS ou lista de IPs, conforme o banco. O tenant vem da URL.

### Mensageria

- **Tópicos:** `<servico>.v1` (ex.: `imoveis.v1`).
  - A chave da mensagem é o id do agregado, o que garante ordem por agregado.
  - O header `tenantId` vai em toda mensagem.
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

### Organização do código: vertical slice

```
servicos/imoveis/
  cadastrarimovel/       # endpoint + request/response (records) + handler
  definirproprietarios/
  publicaroferta/
  buscarimoveis/         # leitura direta com JdbcClient
  contratoativado/       # listener Kafka também é uma fatia
  dominio/               # agregados e regras compartilhadas
  infra/                 # outbox, segurança, config, adaptadores externos
```

- **Uma pasta por caso de uso,** com tudo que ele precisa. As classes são package-private.
- **Regra de negócio fica no domínio.** O handler só orquestra: carrega, chama o agregado, salva, publica.
- **Fatias não chamam umas às outras.** Algo comum vai para `dominio/` (se for regra) ou `infra/` (se for técnico).
- **Acesso a dados:**
  - escrita com regra passa pelo agregado;
  - leitura pode ir direto ao banco, e quem protege é o RLS, já que o `@TenantId` não atua em SQL puro.
- **Domínio puro,** sem Spring nem JPA. Value objects e eventos são records; estados e comandos são sealed interfaces.
- **Portas e adaptadores (hexagonal) só onde há várias implementações reais:**
  - gateway bancário (API REST, CNAB 240, CNAB 400);
  - provedor de assinatura eletrônica;
  - fonte de índices;
  - armazenamento de mídia;
  - consulta de CEP.
- **Evento de domínio ≠ evento de integração.** O mapeamento para Avro fica na infra.

### Testes

- **Domínio:** testes unitários puros, com `@ParameterizedTest` (com `name` legível) e `@CsvSource`/`@MethodSource`. Montar dados com Test Data Builders (`umImovel()...build()`). É onde fica a maior parte dos testes.
- **Fatia:** um teste de integração por caso de uso: MockMvc, Postgres via Testcontainers e verificação do evento no outbox. Usar `@Nested` para agrupar os cenários.
- **Persistência:** sempre Postgres real, nunca H2. Teste obrigatório: o RLS impede leitura entre tenants, verificado com `JdbcTemplate` (que ignora o filtro do Hibernate).
- **Web e segurança:** `jwt()` do `spring-security-test`, com a configuração de segurança importada. Cobrir 401, 403 (tenant fora do token, papel sem permissão), 412 e 422.
- **Kafka:** Testcontainers com Awaitility, e `schema.registry.url=mock://teste`. Testar:
  - produção com o header `tenantId`;
  - idempotência (evento duplicado);
  - eventos fora de ordem.
- **Dublês:** fakes em memória para repositório e publicador de eventos; Mockito só para bordas externas, com `@MockitoBean`.
- **ArchUnit:** o domínio não depende de `org.springframework..` nem de `jakarta.persistence..`.
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
- **Anonimização (LGPD, art. 18, VI):**
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
  - para EM_ANALISE: partes completas, exatamente uma garantia, cônjuge do fiador incluído, percentuais somando 100, valores positivos, reajuste no mínimo anual;
  - para AGUARDANDO_ASSINATURA: garantia aprovada, PDF gerado, hash do conteúdo gravado;
  - para ASSINADO: webhook de todas as assinaturas;
  - para ATIVO: job diário na data de início da vigência;
  - para ENCERRADO: vistoria feita, chaves devolvidas, `AcertoQuitado` recebido.
- **Invariantes legais** (confirmar na lei ao implementar):
  - uma única modalidade de garantia por contrato (Lei 8.245/91, art. 37, parágrafo único);
  - caução em dinheiro de no máximo 3 aluguéis (art. 38, §2º);
  - multa rescisória proporcional ao tempo restante (art. 4º), dispensada em transferência pelo empregador com aviso de 30 dias;
  - prazo de 30 meses ou mais no residencial (art. 46) versus menos de 30 meses (art. 47);
  - aviso de 30 dias no prazo indeterminado (art. 6º);
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
- **Acerto final** a partir de `EncerramentoIniciado`; devolução da caução.

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

- [ ] 0. Infraestrutura: Docker Compose (Postgres, Kafka/Redpanda, Schema Registry, Kafka UI, Keycloak com realm importado, Traefik), starter e schemas.
- [ ] 1. Imóveis
- [ ] 2. Pessoas: primeiro consumidor de eventos de outro serviço.
- [ ] 3. Contratos
- [ ] 4. Financeiro e banco fake
- [ ] 5. Bancário
- [ ] 6. Repasses
- [ ] 7. Índices
- [ ] 8. Documentos e Notificações
- [ ] 9. Relatórios
- [ ] 10. Tenants e BFF completos
- [ ] Opcional: reescrever um serviço já pronto em outra linguagem, sem alterar os demais, para provar que os contratos de eventos estão desacoplados.

**Etapa atual:** 0. Infraestrutura

**Notas da etapa:**
<!-- atualize aqui o que está em andamento e as dúvidas abertas -->
