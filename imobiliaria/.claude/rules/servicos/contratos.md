---
paths:
  - "servicos/contratos/**"
---

# Contratos

Responsabilidade: propostas, contratos de locação e venda, garantias, aditivos, reajuste, renovação, encerramento.

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
