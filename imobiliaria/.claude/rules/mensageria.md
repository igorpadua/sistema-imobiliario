---
paths:
  - "servicos/**"
  - "libs/**"
---

# Mensageria

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
