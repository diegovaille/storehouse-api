# storehouse-api

Contexto específico deste repo. O `CLAUDE.md` guarda-chuva
(`~/Git/pib/CLAUDE.md`) já cobre produto, lista de repos, contrato de API e
higiene de sessão — não repetido aqui.

## O fato mais importante deste repo

**Dois apps completamente não relacionados vivem no mesmo contexto Spring.**
`Application.kt` escaneia `["br.com.storehouse", "br.com.pinguimice"]`.
`br.com.storehouse` é a loja (produtos, vendas, solicitações, estoque) — é o
único pacote relevante para trabalho vindo do frontend da loja. `br.com.pinguimice`
é o admin de um negócio de picolés, sem nenhuma relação com a loja, servido
sob `/api/pinguim-admin/**` (`admin/config`, `admin/controller`,
`admin/entity`, `admin/model`, `admin/repository`, `admin/service`).

Trabalho na loja **nunca** toca `br.com.pinguimice`. Se uma tarefa parecer
exigir mexer lá, é sinal de que a tarefa foi mal entendida — pare e confira.

## Stack

Kotlin 2.0.20 (JVM toolchain 21) · Spring Boot 3.2.5 · Spring Security (JWT +
OAuth2/Google) · JPA/Hibernate · Liquibase (XML) · PostgreSQL · Gradle Kotlin
DSL · JUnit 5 + Cucumber 7 + Testcontainers.

## Camadas (`br.com.storehouse`)

```
api/controller  → recebe HTTP, decide status code, chama service
service         → regra de negócio, transação
data/repository → Spring Data JPA
data/entities   → entidades JPA
data/model      → DTOs (request/response)
```

Mapeamento entidade→DTO é sempre extension function perto do DTO, nunca método
na entidade nem classe "Mapper" — ex. `fun Venda.toResponse(...): VendaResponse`
no fim de `VendaService.kt`.

Um endpoint novo entra em quatro lugares, nessa ordem: entidade em
`data/entities/` (+ migration Liquibase, ver skill `liquibase-migrations`) →
DTO em `data/model/` → método de service em `service/` → método de controller
em `api/controller/`, mapeando a resposta com uma extension function.

## Comandos

```bash
# Postgres local — .env na raiz do repo, compose em local/;
# precisa de --env-file explícito ou as vars saem em branco (docker compose
# não lê .env fora do diretório do compose sozinho)
docker compose --env-file .env -f local/docker-compose.yml up -d db

./gradlew bootRun   # profile default "dev" (application.yml); espera
                     # POSTGRES_HOST/USER/PASSWORD/DB, JWT_SECRET etc. como
                     # variáveis de ambiente do processo — bootRun não lê o
                     # .env sozinho, só o docker compose acima lê
./gradlew test       # JUnit 5 + Cucumber; Testcontainers sobe Postgres
                      # efêmero, não depende do `db` do compose acima
./gradlew build       # build completo
```

Todos os `.feature` do Cucumber (`src/test/resources/features/`) cobrem
`br.com.pinguimice`, não a loja — inclusive `estoque.feature`, que é sobre
estoque de matéria-prima do picolé. Testes da loja são JUnit 5 puro em
`src/test/kotlin/br/com/storehouse/service/`.

## Skills de invariante

Três padrões deste repo têm skill dedicada em `.claude/skills/`:
`estoque-temporal`, `multi-tenant-scoping`, `liquibase-migrations`. Consultar
antes de mexer em estoque/preço, em qualquer query de domínio, ou em
migration nova.

## Bugs conhecidos — não corrigir de passagem

Registrados para não serem confundidos com comportamento esperado. Só
corrigir se a tarefa pedir explicitamente.

- **`SolicitacaoController`** — nenhum método tem guard de role, apesar de o
  commit que o introduziu (`feat(solicitacoes)`) descrever os endpoints como
  admin-only.
