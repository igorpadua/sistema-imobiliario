---
paths:
  - "servicos/**"
  - "libs/**"
---

# Organização do código: package by feature + vertical slice

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
- **Ausência de valor:**
  - campo ou componente que pode faltar fica `null`, e o acessor devolve `null` (ex.: `complemento`, `matricula`, `proprietarios` antes da ativação);
  - `Optional` só como retorno de consulta que pode não encontrar nada (ex.: `findById`);
  - motivo: o javadoc do `Optional` o destina a tipo de retorno, não a campo nem parâmetro, e num record o acessor tem o tipo do componente, então devolver `Optional` exigiria um campo `Optional`.
- **Portas e adaptadores (hexagonal) só onde há várias implementações reais:**
  - gateway bancário (API REST, CNAB 240, CNAB 400);
  - provedor de assinatura eletrônica;
  - fonte de índices;
  - armazenamento de mídia;
  - consulta de CEP.
- **Evento de domínio ≠ evento de integração.** O mapeamento para Avro fica na infra.
