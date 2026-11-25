---
paths:
  - "servicos/relatorios/**"
---

# Relatórios

Responsabilidade: CQRS: projeções desnormalizadas montadas a partir dos eventos.

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
