---
paths:
  - "servicos/pessoas/**"
---

# Pessoas

Responsabilidade: partes envolvidas, qualificação, contatos, dados bancários. Cuida da LGPD.

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
