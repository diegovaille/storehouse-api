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

## O que quebra se violar

Sem escopo por `filialId` vindo do principal, uma filial lê ou escreve dado
de outra filial (ou organização) manipulando o request body ou omitindo o
filtro na query — vazamento entre tenants. `hasAuthority('ADMIN')` faz o
`@PreAuthorize` nunca barrar ninguém autenticado, silenciosamente.

## Violação conhecida

`AdminUsuarioController.kt:20` — `@PreAuthorize("hasAuthority('ADMIN')")`
nunca casa, pelo motivo acima. **Não corrigir de passagem.**

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
