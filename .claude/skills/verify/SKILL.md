---
name: verify
description: Use quando precisar RODAR o storehouse-api de verdade — subir a aplicação contra um Postgres real, aplicar as migrations no boot e exercitar endpoints. Gatilhos — "rodar", "subir", "bootRun", "validar", "verificar", "e2e", "testar de verdade", "localstack".
---

# Rodar o storehouse-api localmente

Testes não sobem a aplicação. As migrations rodam **no boot**, então uma migration quebrada
só aparece aqui — e o `ddl-auto: validate` derruba a aplicação se a entidade divergir do schema.

## Os quatro obstáculos (nesta ordem)

Descobertos rodando de verdade. Sem eles, o boot falha e a mensagem não diz o porquê.

**1. A porta 5432 pode estar ocupada por outro projeto.** Confira antes de subir o compose:
`docker ps --filter 'publish=5432'`. Se estiver, suba um Postgres próprio noutra porta e
sobrescreva a URL inteira — a porta está hardcoded no `application-dev.yml`, então mexer só
no `POSTGRES_HOST` não resolve:

```bash
docker run -d --name pib-db -p 5433:5432 \
  -e POSTGRES_DB=estoque -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres postgres:16
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5433/estoque'
```

**2. `bootRun` não lê o `.env`.** Exporte antes: `set -a; source .env; set +a`.

**3. O `UploadTestRunner` (`@Profile("dev")`) faz um upload REAL no S3 a cada boot** e derruba
a aplicação se o bucket não existir. O perfil `dev` aponta para `localhost:4566` — que pode ser
o localstack de outro projeto. Suba um próprio e crie os buckets:

```bash
docker run -d --name pib-s3 -p 4567:4566 -e SERVICES=s3 localstack/localstack:4.12.0
AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_DEFAULT_REGION=us-east-1 \
  aws --endpoint-url=http://localhost:4567 s3 mb s3://storehouse-images
# idem para despesas-files
```

**Não sobrescreva só o `endpoint` de um bucket por env var** (`PROJECT_STORAGE_BUCKETS_0_ENDPOINT`):
o Spring reconstrói o item da lista inteiro e perde o `provider`, que é `lateinit` →
`UninitializedPropertyAccessException`. Passe a **lista completa** num arquivo de override:

```bash
./gradlew bootRun --args="--spring.profiles.active=dev --openai.api-key=dummy \
  --server.port=8081 --spring.config.additional-location=file:/caminho/override.yml"
```

**4. `openai.api-key` é obrigatória** para o contexto subir (`InsightService`). Um valor dummy
serve, desde que você não chame `/api/insights`.

## Autenticar

A senha do seed é um hash bcrypt irreversível. Num banco descartável, troque por uma conhecida:

```bash
HASH=$(htpasswd -bnBC 10 "" 'senha123' | tr -d ':\n' | sed 's/^\$2y/\$2a/')
docker exec pib-db psql -U postgres -d estoque -c "update usuario set password='$HASH' where username='admin';"
```

O fluxo tem **três passos** (todos com body JSON, nenhum usa header `Authorization` no primeiro):
`POST /api/auth/login {username,password}` → `tempToken`
→ `POST /api/auth/organizacoes {tempToken}` → lista de orgs/filiais
→ `POST /api/auth/token {tempToken,organizacaoId,filialId}` → o JWT final.

IDs semeados: org `20587698-1b67-4cbb-8a08-d7e9fe56a77d`, filial Store
`e741e0b4-02f9-4e6e-b3c3-4318d36477b3`, tipo Livro `1b4b2f66-6b55-4ac8-90b0-027fb7d9c1fe`.

**Atenção:** o seed cadastra o usuário `vendedor` com perfil **ADMIN**. Para testar guard de
role, corrija o perfil no banco antes — senão você testa um admin achando que é vendedor.

## Testar uma migration como um upgrade de produção

Banco limpo prova pouco. O risco real é a migration rodando sobre dados existentes:

1. `git worktree add /tmp/old origin/main` e suba dali → cria o schema como está em produção.
2. Suje os dados no `psql` (**use `docker exec -i`** — sem o `-i` o heredoc não chega ao psql).
3. Volte para a branch e suba → é aqui que a migration nova roda sobre dados reais.

Foi assim que se descobriu que a migration 4.5 destruía o único estado aberto de produtos com
`estado_atual_id` nulo.

## Gotcha: entidades com coluna jsonb

`TipoProduto.campos` é `String` com `@Column(columnDefinition = "jsonb")`. O Hibernate 6 manda
varchar e o Postgres recusa (`column "campos" is of type jsonb but expression is of type
character varying`). Em produção nunca aparece porque os tipos vêm do seed do Liquibase, nunca
via JPA. Em teste, **carregue o tipo semeado em vez de criar um**.

## Limpar

```bash
docker rm -f pib-db pib-s3
git worktree remove /tmp/old --force
```
