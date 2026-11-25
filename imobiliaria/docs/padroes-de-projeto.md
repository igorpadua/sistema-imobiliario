# Padrões de projeto

Um padrão entra quando resolve um problema real do projeto, não por antecipação. A tabela mostra onde eles provavelmente vão aparecer; confirme cada um quando o problema surgir.

| Padrão | Onde provavelmente aparece | Etapa |
|---|---|---|
| Aggregate, Value Object e Repository (DDD tático) | `Imovel`, `Oferta`, `Endereco`; repositórios Spring Data | 2 |
| Factory Method (método de fábrica estático) | criação de agregados que já nascem válidos | 2 |
| Transactional Outbox | evento gravado na mesma transação do dado | 2 |
| Test Data Builder | montagem de dados nos testes (`umImovel()...build()`) | a partir da 2 |
| Idempotent Consumer | consumidores Kafka | 4 |
| Anti-Corruption Layer | tradução de eventos de outros serviços para o modelo local; Bancário normalizando tudo em `PagamentoRecebido` | 4 e 7 |
| Command e State | comandos do contrato (`sealed interface Comando`) e máquina de estados. As regras de Contratos já escolheram função pura com sealed interface; vale comparar com o State clássico do GoF | 5 |
| Specification | condições de cada transição do contrato, combináveis entre si | 5 |
| Saga coreografada | encerramento, acerto final e repasse, passando por vários serviços | 5 a 8 |
| Strategy | multa e juros, índice de reajuste, canal de notificação | 5, 6 e 10 |
| Adapter + Strategy | `GatewayBancario` (API, CNAB 240, CNAB 400) escolhido por tenant; fonte de índices | 7 e 9 |
| Template Method | estrutura comum de leitura e geração dos arquivos CNAB 240 e 400 | 7 |
| CQRS e projeções | Relatórios | 11 |
| BFF e API Composition | agregação de dados para o front | 12 |
