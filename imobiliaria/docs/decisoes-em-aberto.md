# Decisões em aberto

Itens que ainda precisam de decisão, agrupados pela etapa em que passam a bloquear o trabalho. Quando a etapa começa, os itens dela viram issues de decisão e saem desta lista. Ao fechar a issue, a decisão entra no arquivo de regras correspondente em `.claude/rules/`.

Algumas etapas trazem também o escopo e o critério de pronto, que passam para a issue pai quando ela for criada.

## Etapa 2: Imóveis

As decisões desta etapa viraram issues com o rótulo `decisão`, sub-issues da #9.

## Etapa 3: Identidade

Escopo: Keycloak (realm importado com dois tenants e client de desenvolvimento) e Traefik. Pronto quando o Imóveis aceita um token real obtido pelo client de desenvolvimento e recusa um `X-Tenant-Id` fora do token.

- **Roteamento no Traefik:** por host (`keycloak.localhost`, `api.localhost`) ou por caminho (`localhost/auth`). Confira antes se `getent hosts keycloak.localhost` resolve para 127.0.0.1.
- **Armazenamento do Keycloak:** efêmero, com o JSON do realm como fonte da verdade, ou banco próprio.

## Etapa 4: Pessoas

Escopo: primeiro consumidor de eventos de outro serviço. Aqui o starter interno e os convention plugins são extraídos.

- **Critério de idempotência:** tabela de eventos processados × upsert por versão. Sugestão: upsert para projeções de estado; tabela para consumidores com efeito colateral.
- **DLQ por tópico ou por (tópico, consumidor):** com dois consumidores no mesmo tópico, uma DLQ compartilhada não indica quem falhou, e o reprocessamento entregaria a mensagem aos dois.
- **Índice cego:** normalização antes do HMAC (pontuação, maiúsculas no CNPJ alfanumérico) e chave por tenant ou global (a global permite correlacionar a mesma pessoa entre tenants).
- **`pessoaId` ainda não copiado para o Imóveis:** o `PUT` de proprietários pode chegar antes de a pessoa recém-criada estar na cópia local. Opções: 422 com um `type` próprio, para o cliente tentar de novo, ou aceitar e bloquear a ativação enquanto houver proprietário não confirmado. Definir também o efeito do `PessoaAnonimizada` e do tombstone na cópia.
- **Entrada da observabilidade:** OpenTelemetry, Grafana, Tempo e Loki estão na stack, mas em nenhuma etapa. Sugestão: na etapa 4, com o primeiro fluxo entre dois serviços, antes das sagas das etapas 5 a 8. O outbox quebra a propagação do trace (o evento é publicado depois, por outro processo): o contexto precisa ser gravado na linha do outbox e repassado nos headers do Kafka.

## Etapa 5: Contratos

Escopo: máquina de estados e regras da Lei do Inquilinato.

- **Estados como sealed interface × entidade JPA:** um estado modelado como sealed interface não vira coluna diretamente. Opções: enum simples, ou sealed interface persistida por um conversor.
- **Token de serviço × `X-Tenant-Id`:** o token de client credentials não tem o claim `tenants`; definir como o tenant é aceito nessas chamadas.
- **LGPD × snapshots e retenção infinita:** nomes das partes ficam nos snapshots de Contratos e em tópicos sem expiração, fora do alcance da anonimização. Opções: eventos só com ids, crypto-shredding, ou guarda justificada pelo art. 16, I.
- **Encerramento com dívida:** o que acontece se `AcertoQuitado` nunca chegar; quando o imóvel volta a VAGO (na entrega das chaves ou no ENCERRADO).
- **Ativação e cancelamento:** contrato assinado depois da data de início da vigência; se ASSINADO → CANCELADO é permitido ou exige distrato.
- **Locação não residencial** (ação renovatória, art. 51): dentro ou fora do escopo.
- **Reajuste:** qual janela de 12 meses (o índice é publicado com defasagem) e se o acumulado vem do produto das taxas mensais ou do número-índice (podem divergir na última casa).

## Venda (etapa 5 ou depois do marco)

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
- **Ofertas de locação e venda ao mesmo tempo:** o índice único por modalidade permite as duas, mas não define como uma afeta a outra: o que acontece com a oferta de locação quando a proposta de venda é aceita ou a venda é concluída, e com a oferta de venda quando a locação é fechada (o comprador passa a herdar o inquilino). A condição do índice parcial e o status RESERVADA estão na issue #26.

## Etapas 6 a 8: Financeiro, Bancário e Repasses

Escopo: a etapa 6 traz o Financeiro e o banco fake; a 7, os adaptadores de API e de CNAB.

- **Arredondamento:** modo (`HALF_EVEN` ou `HALF_UP`) e quem fica com o centavo que sobra no rateio do repasse.
- **Vencimento** em fim de semana ou feriado; tamanho da janela móvel de parcelas.
- **Datas:** tipo da competência (`YearMonth`?) e fuso dos jobs (`America/Sao_Paulo`?). O `ocorridoEm` foi tratado na etapa 2 (#16).
- **Eventos sem agregado,** como `PagamentoRecebido` (etapa 7): qual id vira a chave da mensagem e o que `versao` significa no envelope.

## Etapa 10: Documentos e Notificações

- **Servidor de objetos:** a edição comunitária do MinIO foi reduzida no fim de 2025 (confirmar a situação atual). Escolher entre o MinIO e uma alternativa compatível com S3 antes de colocá-lo no compose.

## Etapa 11: Relatórios

- **DIMOB e informe de rendimentos × eventos sem documento:** Relatórios precisa de CPF/CNPJ, que os eventos não trazem. Opções: consulta interna a Pessoas (ampliando a exceção às chamadas síncronas) ou geração em outro serviço.

## Etapa 12: Tenants e BFF

- **Cadastro de tenants × "não existe entidade global":** a tabela de tenants é uma exceção ou também fica sob RLS.
- **Portal do cliente:** como o usuário do Keycloak se liga à `Pessoa`, e se o client separado implica outro BFF.

## Depois do marco: Auditoria

Serviço que consome os eventos de todos os serviços e guarda a trilha de alterações (antes, depois e autor). O autor no envelope é tratado na etapa 2 (#15).

- **Destino da trilha:** tabela append-only no Postgres, com RLS, ou Elasticsearch/OpenSearch, com busca melhor mas sem RLS (o isolamento entre tenants ficaria só no código).
- **Antes e depois:** o serviço guarda o último snapshot de cada agregado e calcula a diferença, ou o evento passa a levar os campos alterados.
- **Campos sensíveis:** o que não vai nos eventos (documento, dados bancários) aparece só como "alterado", sem valores. Confirmar se basta.
- **Auditoria de leitura de dados pessoais:** hoje exigida em Pessoas e na consulta interna. Definir se vira evento (ex.: `DocumentoConsultado`) consumido pela Auditoria ou se fica local em cada serviço.
- **Histórico anterior ao serviço:** tópicos sem retenção infinita (ex.: `imoveis.v1`) perdem os eventos antigos. Definir retenção maior ou aceitar que a trilha começa quando o serviço entra.
- **Acesso e guarda:** quais papéis leem a trilha e por quanto tempo ela é guardada. O `sub` do funcionário também é dado pessoal (LGPD).
