---
paths:
  - "servicos/imoveis/**"
---

# Imóveis

Responsabilidade: catálogo, proprietários com percentual de posse, captação, ofertas, mídias, busca.

- **Agregado `Imovel`:** código legível único por tenant (gerado por uma tabela de sequência por tenant), tipo, uso, endereço (value object com latitude e longitude), características, matrícula, inscrição de IPTU.
- **Tipo e uso:** dois enums separados, porque uma casa pode ter uso comercial.
  - tipo: CASA, APARTAMENTO, SALA_COMERCIAL, TERRENO. Um só por imóvel, porque descreve o que ele é fisicamente;
  - uso: RESIDENCIAL, COMERCIAL, MISTO. `MISTO` cobre o imóvel que serve aos dois (sobrado com loja no térreo). Com dois usos, ele equivale a um conjunto e custa uma coluna; se surgir um terceiro uso, trocar por um conjunto (`Set<Uso>`);
  - o uso diz para que o imóvel serve. O regime da Lei do Inquilinato (residencial ou não residencial) vem da finalidade do contrato;
  - as listas crescem quando houver requisito real (ex.: loja, galpão, chácara).
- **Campos exigidos na criação:**
  - obrigatórios na fábrica: código legível (vem pronto da sequência por tenant), tipo, uso, endereço e características. Sem eles o imóvel não entra na busca nem nas ofertas;
  - opcionais: matrícula e inscrição de IPTU, porque o corretor costuma cadastrar antes de ter os documentos. Vazio ou só com espaços vira `null`, como no complemento. Continuam opcionais na ativação até haver requisito real (ex.: a venda);
  - proprietários: ausentes (campo nulo) na criação, exigidos na ativação.
- **Características mínimas:** quartos, suítes, banheiros e vagas, obrigatórios e ≥ 0; as suítes contam entre os quartos (suítes ≤ quartos). Área útil opcional, em m², > 0 quando informada, em `BigDecimal` com no máximo 2 casas decimais significativas (`numeric(10,2)`): casas a mais são recusadas, não arredondadas, zeros à direita não contam e o valor é guardado sempre com escala 2 (`72.5` vira `72.50`). A área vai até 99.999.999,99, o limite da coluna, e acima disso é recusada no domínio para não virar erro 500 no `INSERT`. O conjunto cresce quando a busca ou os portais pedirem.
- **Endereço:**
  - logradouro, número, bairro, cidade, UF e CEP obrigatórios; complemento opcional, e vazio ou só com espaços vira `null` (uma única forma de "sem complemento");
  - textos em `String` (logradouro, número, complemento, bairro, cidade e CEP), com os espaços das pontas removidos no construtor (`strip`): " Rua das Flores " vira "Rua das Flores";
  - número em `String`, porque "S/N" existe;
  - CEP em `String` (muitos começam com zero), com 8 dígitos e guardado sem hífen. A entrada aceita "01310100" ou "01310-100", com o hífen só entre o 5º e o 6º dígito ("0131-0100" é recusado), e o construtor normaliza para "01310100";
  - UF como enum com as 27 siglas;
  - latitude e longitude opcionais, mas sempre em par, em `Double` (o PostGIS guarda `float8`; a proibição de `double` vale para dinheiro). `NaN` e infinito são recusados. Imóvel sem coordenadas fica fora da busca geográfica.
- **Invariante × completude:**
  - invariante vale em qualquer `situacao` e fica no value object, validada no construtor (ex.: contagens ≥ 0);
  - completude é o mínimo para ir a público e é conferida na transição para ATIVO (ex.: proprietários definidos), porque o RASCUNHO aceita cadastro incompleto;
  - regras de completude por tipo de imóvel ficam para quando houver requisito real.
- **Agregado `Oferta`:** separado do imóvel, com uma oferta por modalidade (`LOCACAO`/`VENDA`), garantida por índice único parcial. Mudar o preço encerra a oferta atual e cria outra, o que gera histórico.
- **Status separados:**
  - `situacao`: RASCUNHO, ATIVO, INATIVO (muda por ação do usuário);
  - `ocupacao`: VAGO, LOCADO (muda só por eventos de Contratos);
  - status da oferta: ATIVA, RESERVADA, PAUSADA, ENCERRADA.
- **Ciclo da `situacao`:**
  - RASCUNHO → ATIVO e INATIVO → ATIVO exigem proprietários definidos. A soma 100 é invariante da lista (value object), então um imóvel ativo nunca tem soma diferente de 100.
  - RASCUNHO → INATIVO descarta o cadastro. Não há exclusão física de imóvel nem `DELETE` na API. Um estado `DESCARTADO`, sem volta, fica para quando houver necessidade.
  - Inativar é recusado com `ocupacao` LOCADO (implementado na etapa 5, com o listener `contratoativado`). Eventos de Contratos não são recusados, então INATIVO + LOCADO pode surgir por atraso de evento e é tolerado.
- **Proprietários:**
  - lista num value object, com percentual de posse em quatro casas decimais (`numeric(7,4)`). Um valor com mais de quatro casas significativas é recusado, não arredondado; zeros à direita não contam (`33.33330` vale `33.3333`). O percentual é guardado sempre com escala 4;
  - invariantes da lista: não vazia, `pessoaId` e percentual obrigatórios, cada percentual > 0, soma exatamente 100 e cada `pessoaId` uma única vez (repetido costuma ser erro de seleção e esconde outro proprietário). Com todos > 0 e soma 100, nenhum passa de 100, então não há checagem de ≤ 100;
  - "sem proprietários" é o campo nulo no `Imovel`, não uma lista vazia: o imóvel nasce sem eles, e a ativação confere se o campo está preenchido;
  - o `PUT` substitui a lista inteira. Lista vazia é recusada, então não há como remover todos os proprietários;
  - o `pessoaId` não é validado até a etapa 4. A partir dela, é conferido numa cópia local de `pessoas.v1` (só `id`, `tenant_id` e `versao`), que só o consumidor escreve e para a qual os proprietários não têm foreign key.
- **Mídia:** URL pré-assinada do MinIO, upload direto pelo front, confirmação, e miniaturas geradas de forma assíncrona. Fica para depois do marco, numa etapa própria, porque não faz parte do fluxo do marco.
- **Busca:** Postgres (índices, `pg_trgm`, PostGIS). OpenSearch é uma evolução opcional.
- **Venda de imóvel alugado:** o inquilino tem direito de preferência (Lei 8.245/91, arts. 27 e 28; prazo de 30 dias).
- **Extras futuros:** controle de chaves, feed XML para portais, CEP via adaptador.
