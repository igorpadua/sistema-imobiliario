---
paths:
  - "servicos/**"
  - "libs/**"
  - "infra/**"
---

# Autenticação e autorização

- **Keycloak:** um único realm, `imobiliarias`.
- **Tenants e papéis como grupos:** caminho `/tenants/{tenantId}/{papel}`. Um mapper *Group Membership*, com caminho completo, gera o claim `tenants`. A audiência é `imob-api`.
- **Papéis:**
  - equipe da imobiliária: `admin`, `gerente`, `corretor`, `financeiro`, `atendente`;
  - portal do cliente: `inquilino`, `proprietario`, em um client separado.
- **BFF:** fluxo Authorization Code com PKCE. Os tokens ficam no Redis; o navegador recebe só o cookie `__Host-sid` (`HttpOnly`, `Secure`, `SameSite=Lax`). O BFF guarda o tenant ativo e envia `X-Tenant-Id` em cada chamada aos serviços.
- **Cada serviço valida o JWT** (assinatura, `iss`, `aud`, `exp`) e só aceita o `X-Tenant-Id` se o tenant estiver no claim `tenants`. Os papéis valem só para o tenant ativo: quem é `admin` em um tenant e `corretor` em outro tem, em cada chamada, apenas os papéis do tenant do `X-Tenant-Id`.
- **Chamadas entre serviços:** token de client credentials, com papel `servico`.
- **Desenvolvimento antes do BFF:** um client público, só de desenvolvimento, com Authorization Code + PKCE, usado a partir de um cliente HTTP (Bruno, Postman ou Insomnia). O token dele também precisa da audiência `imob-api`. O realm importado traz dois tenants fixos e usuários por papel, incluindo um usuário em dois tenants com papéis diferentes. Até a etapa 12, os tenants existem só por esse import e por carga inicial. Antes da etapa 3, que traz o Keycloak, a segurança é exercitada só nos testes, com `jwt()`.
- **Armadilha do Docker:** o `iss` do token é a URL pública do Keycloak. Configure `KC_HOSTNAME` com a URL pública e `KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true`. Nos serviços, use `jwk-set-uri` com o endereço interno e valide o issuer explicitamente com a URL pública.
- **Webhooks de banco** não passam pelo Keycloak: a autenticação é por HMAC, mTLS ou lista de IPs, conforme o banco. O tenant vem da URL.
