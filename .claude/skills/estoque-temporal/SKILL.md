---
name: estoque-temporal
description: Use quando mexer em estoque, preço, precoCusto, ProdutoEstado, venda ou cancelar venda — estoque/preço/custo vivem numa cadeia temporal de ProdutoEstado, nunca são mutados em lugar.
---

## A regra

`estoque`, `preco` e `precoCusto` vivem em `ProdutoEstado`, nunca em
`Produto`. `Produto.estadoAtual` aponta para o estado vigente. Qualquer
mudança nesses campos segue sempre a mesma sequência:

1. Fechar o estado atual: `estadoAtual.dataFim = LocalDateTime.now()`.
2. Criar um `ProdutoEstado` novo com os valores atualizados.
3. Repontar `produto.estadoAtual` para o novo estado.

Nunca `estadoAtual.estoque += x` ou `=` in-place. O padrão correto está em
`VendaService.kt` `registrarVenda` (linhas 61-94): fecha `dataFim`, cria
`ProdutoEstado` novo, repontar `estadoAtual`.

## A checagem

```bash
cd ~/Git/pib/storehouse-api
rg -n -P --glob 'src/main/**/*.kt' '\.estoque\s*(\+=|-=|(?<![=!<>])=(?!=))'
```

Saída esperada hoje — exatamente uma linha, a violação conhecida:

```
src/main/kotlin/br/com/storehouse/service/VendaService.kt:257:            estadoAtual.estoque += item.quantidade
```

Qualquer linha adicional nessa saída é um bug novo introduzido na sessão
atual — investigar antes de prosseguir.

## O que quebra se violar

Mutar o estado em lugar apaga o histórico temporal: não dá mais para saber
qual era o estoque/preço/custo em um instante passado, o que quebra
relatórios que recalculam `precoCusto`/`estoque` histórico a partir da
cadeia de `ProdutoEstado` (ex.: `toResponse(relatorio = true)` em
`VendaService.kt`, que lê `it.produto.estadoAtual!!.estoque` esperando que
esse valor reflita a cadeia correta, não um remendo).

## Violação conhecida

`VendaService.kt:257` — `cancelarVenda` faz
`estadoAtual.estoque += item.quantidade`, restaurando o estoque por mutação
direta em vez de fechar o estado atual e criar um novo. Isso é uma violação
conhecida do próprio padrão que `registrarVenda` segue corretamente
(linhas 61-94), logo acima no mesmo arquivo. **Não corrigir de passagem** —
só se a tarefa pedir explicitamente.

## Por que não há exclusões nesta checagem

O regex já usa negative lookahead (`(?!\s*estadoAtual)`) para não acusar a
própria criação do `ProdutoEstado` novo (`estoque = estadoAtual.estoque - ...`),
que é o padrão correto. Não há mais nenhuma exclusão: qualquer outra
ocorrência de `.estoque +=`, `.estoque -=` ou `.estoque =` fora desse
contexto é, por definição, uma mutação em lugar.
