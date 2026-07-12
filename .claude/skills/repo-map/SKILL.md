---
name: repo-map
description: Use quando precisar navegar o storehouse-api, achar onde fica uma classe/endpoint, entender a estrutura de pastas ou decidir por onde começar uma tarefa — trigger words: onde fica, estrutura, navegar, arquivo, pacote, camada.
---

# Mapa do repo — storehouse-api

Lembrete: `br.com.storehouse` é a loja; `br.com.pinguimice` é um app não
relacionado (admin de picolés) no mesmo contexto Spring. Ver `CLAUDE.md` na
raiz do repo. Esta skill só mapeia `br.com.storehouse`.

## Árvore anotada — `src/main/kotlin/br/com/storehouse/`

```
storehouse/
├── api/
│   ├── controller/     8 controllers REST (Auth, Produto, Venda, VendaV2,
│   │                   Solicitacao, TipoProduto, Insight, AdminUsuario)
│   ├── handler/        GlobalExceptionHandler, OAuth2SuccessHandler
│   └── security/       JwtUtils + security/filters/JwtAuthenticationFilter.kt
├── config/             SecurityConfig, JwtConfig, WebClientConfig
├── constants/          ErrorMessages (mensagens de exceção centralizadas)
├── data/
│   ├── entities/       14 entidades JPA (Produto, ProdutoEstado, Venda,
│   │                   VendaItem, VendaPagamento, Solicitacao, Usuario,
│   │                   Organizacao, Filial, Perfil, ...)
│   ├── enums/          StatusSolicitacao, TipoPagamento, TipoProdutoEnum
│   ├── model/          DTOs de request/response (um por fluxo, ex.
│   │                   VendaRequest/VendaResponse, ProdutoDto/ProdutoResponse)
│   └── repository/     14 interfaces Spring Data JPA, uma por entidade
├── exceptions/         EntidadeNaoEncontradaException, EstadoInvalidoException,
│                       RequisicaoInvalidaException
├── logging/            LogCall (annotation) + LoggingAspect (AOP)
├── runner/             UploadTestRunner (utilitário de linha de comando)
├── service/             11 services — regra de negócio e transação
└── storage/             abstração de storage (AWS S3 / Oracle OCI), usada
                          por upload de imagem de produto
```

Camadas e onde entra código novo: ver `CLAUDE.md` na raiz do repo.

## Testes

- `src/test/kotlin/br/com/storehouse/service/` — testes JUnit 5 da loja
  (ex. `VendaServiceVoucherTest`, `SolicitacaoServiceTest`,
  `RelatorioServiceTest`). Este é o padrão para testar service da loja.
- `src/test/kotlin/br/com/storehouse/data/model/` — contract test de DTO
  (`VendaResponseContractTest`).
- `src/test/kotlin/br/com/storehouse/cucumber/` — step defs Cucumber. Cobrem
  só `br.com.pinguimice` (auth, despesa, estoque de matéria-prima, produção,
  região, sabor) — nenhum `.feature` cobre a loja hoje.
- `src/test/resources/features/*.feature` — os `.feature` acima (todos
  pinguimice).
- `src/test/kotlin/br/com/pinguimice/` — testes unitários do pinguimice
  (fora do escopo de trabalho na loja).

## Pontos de partida

```bash
# o que mudou nesta branch em relação a origin/main
git diff --name-only origin/main...HEAD

# achar onde uma classe/endpoint está definido
rg "class VendaService" src/main/kotlin/br/com/storehouse
rg "@RequestMapping" src/main/kotlin/br/com/storehouse/api/controller
```
