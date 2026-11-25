---
paths:
  - "servicos/bancario/**"
---

# Bancário

Responsabilidade: adaptadores de API REST e CNAB 240/400, webhooks, retorno. Normaliza tudo em `PagamentoRecebido`. Inclui um "banco fake" para testes.

- **Porta `GatewayBancario`** (registrar, consultar e cancelar boleto), com adaptadores API REST, CNAB 240 e CNAB 400, configurados por tenant.
- **Idempotência** pela combinação banco + nosso número + ocorrência.
- **Dados do pagador** (nome, documento, endereço) obtidos da consulta interna de Pessoas.
- **Banco fake:** simula a API, lê remessas e gera arquivos de retorno com pagamentos.
