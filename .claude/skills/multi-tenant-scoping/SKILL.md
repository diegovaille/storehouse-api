---
name: multi-tenant-scoping
description: Use quando criar ou revisar endpoint, controller, repository, query de domínio, ou anotação de role/autorização — todo dado é escopado por filial, tirado do @AuthenticationPrincipal, nunca do request body; role check usa hasRole, nunca hasAuthority.
---

## A regra

O modelo é `Organizacao → Filial`. Todo acesso (leitura e escrita) a dado de
domínio é escopado por `filialId`, tirado do
`@AuthenticationPrincipal UsuarioAutenticado` — **nunca** de um campo no
request body (o cliente não pode escolher a filial de outro tenant). Rotas
de escrita declaram role com `hasRole("ADMIN")`. **Nunca** `hasAuthority`:
o filtro concede `ROLE_${perfil.uppercase()}` (`JwtAuthenticationFilter.kt:69`),
ou seja `ROLE_ADMIN` — `hasAuthority` compara sem o prefixo `ROLE_` e por
isso nunca casa, deixando a rota sem guard (`SecurityConfig.kt:52` usa
`hasRole` corretamente para `/api/admin/**`).

## A checagem

```bash
cd ~/Git/pib/storehouse-api

# (a) controller de escrita sem escopo de filial
rg -l --glob 'src/main/kotlin/br/com/storehouse/api/controller/*.kt' '@(Post|Put|Patch|Delete)Mapping' \
  | grep -v 'AuthController.kt' | while read f; do
      grep -q "@AuthenticationPrincipal" "$f" || echo "VIOLACAO: $f"
    done

# (b) repository de domínio sem escopo de filial
rg -n --glob 'src/main/kotlin/br/com/storehouse/data/repository/*.kt' '^\s*fun find' \
  | grep -vE 'Usuario|Perfil|Organizacao' | grep -vE 'FilialId|ProdutoId'

# (c) autoridade que nunca casa
rg -n --glob 'src/main/**/*.kt' 'hasAuthority'
```

Saída esperada hoje: (a) e (b) limpas (sem output). (c):

```
src/main/kotlin/br/com/storehouse/api/controller/AdminUsuarioController.kt:20:    @PreAuthorize("hasAuthority('ADMIN')")
```

Qualquer output novo em (a) ou (b) é violação real.

## Limites destas checagens — o que elas NÃO veem

As três checagens estão limpas hoje, e o que elas pegam elas pegam de
verdade. Mas cada uma é mais estreita que a regra que enforça. Isto é
deliberado — estreita-mas-silenciosa vale mais que ampla-mas-barulhenta, e
uma checagem em que ninguém confia não checa nada. O preço é este, e está
escrito para que ninguém confie nelas mais do que merecem:

- **(b) o `grep -vE 'Usuario|Perfil|Organizacao'` casa o CAMINHO do arquivo,
  não só a assinatura do método.** Um `fun findByUsuarioId(...)` adicionado a
  um repository qualquer é engolido em silêncio — o filtro não distingue "é
  um repository de identidade" de "tem a palavra Usuario na linha".
- **(b) só casa `^\s*fun find`.** `countBy`, `existsBy`, `deleteBy` e métodos
  com `@Query` são invisíveis para ela — um `deleteBy...` sem escopo de
  filial passa liso.
- **(a) é por ARQUIVO, não por método.** Um controller passa se
  `@AuthenticationPrincipal` aparecer em qualquer lugar dele. Um método de
  escrita novo, sem escopo, adicionado a um controller já escopado, passa
  silenciosamente.

## A rota de escrita sem guard de role NÃO tem checagem mecânica

A parte da regra que diz "rota de escrita declara role com `hasRole(...)`" é
a única sem nenhuma checagem. As três checagens acima não a enxergam: (c) só
acha `hasAuthority` onde ele **existe**; um `@PostMapping` com guard **nenhum**
não produz output em lugar nenhum.

Isto é uma **lacuna aceita, não um esquecimento.** Não adicionar um
`rg -L PreAuthorize` por cima dos controllers: a maioria deles legitimamente
não tem `@PreAuthorize` (são autenticados-mas-não-admin-only), então essa
checagem acusaria todos e viraria ruído — e uma checagem que grita lobo é
pior que lacuna nenhuma, porque ensina a ignorar a saída.

Enquanto não houver um jeito de expressar "esta rota deveria ser admin-only"
no código, esta regra se verifica **lendo o controller**: ao criar ou revisar
um `@Post/@Put/@Patch/@DeleteMapping`, decidir explicitamente se ele é
admin-only e, se for, anotar `@PreAuthorize("hasRole(\"ADMIN\")")`. Nenhum
comando vai lembrar por você.

## O que quebra se violar

Sem escopo por `filialId` vindo do principal, uma filial lê ou escreve dado
de outra filial (ou organização) manipulando o request body ou omitindo o
filtro na query — vazamento entre tenants. `hasAuthority('ADMIN')` faz o
`@PreAuthorize` nunca barrar ninguém autenticado, silenciosamente.

## Violações conhecidas — **não corrigir de passagem**

Duas, hoje. Ambas são **conhecidas**: registradas para não serem confundidas
com comportamento esperado, e **a não corrigir de passagem** — só mexer se a
tarefa pedir explicitamente.

1. **`AdminUsuarioController.kt:20`** — `@PreAuthorize("hasAuthority('ADMIN')")`
   nunca casa, pelo motivo acima (o filtro concede `ROLE_ADMIN`). A rota fica
   sem guard efetivo. Pega pela checagem (c).

2. **`SolicitacaoController.kt`**
   (`src/main/kotlin/br/com/storehouse/api/controller/SolicitacaoController.kt`)
   — **duas rotas de escrita e zero guard de role**: `@PostMapping` (`:17`) e
   `@PatchMapping("/{id}")` (`:31`), nenhum `@PreAuthorize` em lugar nenhum do
   arquivo. O commit que introduziu o controller (`feat(solicitacoes)`)
   descreve os endpoints como admin-only, mas nada no código os restringe:
   hoje qualquer usuário autenticado (inclusive `VENDEDOR`) cria e altera
   solicitação. O escopo de filial está correto (o controller usa
   `@AuthenticationPrincipal`, por isso a checagem (a) passa) — o que falta é
   só a role. **Nenhuma das três checagens pega esta violação**, pelo motivo
   da seção anterior; ela só existe porque alguém leu o controller.

## Exclusões — deliberadas, não remover

- **(a) `AuthController.kt`**: rotas (`/api/auth/**`) são login/token,
  legitimamente públicas — ainda não há principal autenticado nesse ponto.
- **(b) `Usuario|Perfil|Organizacao`**: identidade vive ACIMA do tenant —
  usuário pertence a uma organização, não a uma filial isolada; escopar
  por `filialId` não faz sentido de domínio aqui.
- **(b) `FilialId|ProdutoId`**: nome já contém `FilialId` (escopo direto)
  ou `ProdutoId` (`ProdutoDescricaoRepository` escopa indiretamente via
  `produtoId`, cujo produto já é filial-scoped — não precisa repetir).

Exclusão nova exige a mesma justificativa escrita aqui, ou a checagem
apodrece silenciosamente.
