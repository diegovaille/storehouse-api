---
name: estoque-temporal
description: Use quando mexer em estoque, preço, precoCusto, ProdutoEstado, venda ou cancelar venda — estoque/preço/custo vivem numa cadeia temporal de ProdutoEstado, nunca são mutados em lugar, e só podem ser escritos por ProdutoEstadoService.
---

## A regra

**Ninguém constrói `ProdutoEstado` fora do `ProdutoEstadoService`.**

`estoque`, `preco` e `precoCusto` vivem em `ProdutoEstado`, nunca em
`Produto`. `Produto.estadoAtual` aponta para o estado vigente (`data_fim IS
NULL`). Qualquer mudança nesses campos segue sempre a mesma sequência, e
`ProdutoEstadoService` é o único lugar do código que a executa:

1. Fechar o estado atual: `estadoAtual.dataFim = LocalDateTime.now()`.
2. Criar um `ProdutoEstado` novo com os valores atualizados.
3. Repontar `produto.estadoAtual` para o novo estado.

Antes da centralização, essa sequência era reimplementada à mão em seis
call sites — e um deles (`cancelarVenda`) fazia errado, mutando
`estadoAtual.estoque` em lugar. **Esse bug está corrigido**: `cancelarVenda`
hoje chama `ProdutoEstadoService.aplicarDelta`, e `VendaService` não tem
mais nenhum código de estoque próprio. A regra antiga ("nunca
`estadoAtual.estoque += x`") virou uma garantia estrutural: não é mais
"cuidado para não fazer isso na mão", é "só existe um lugar que sabe fazer
isso, e não é aqui".

## As três camadas de defesa

Nenhuma camada sozinha bastaria — cada uma cobre o furo que a anterior
deixa passar:

1. **Escritor único** (`ProdutoEstadoService`) — garante que a SEQUÊNCIA
   fechar→criar→repontar é sempre seguida da mesma forma. Não impede,
   sozinho, que duas transações concorrentes leiam o mesmo estoque "atual"
   ao mesmo tempo e ambas achem que há saldo.
2. **Lock pessimista de linha** (`ProdutoRepository.findByIdForUpdate`,
   `@Lock(PESSIMISTIC_WRITE)`, usado sob `Propagation.MANDATORY`) — serializa
   escritores concorrentes do MESMO produto: a segunda transação só lê o
   estoque depois que a primeira commitou. Sem isso, duas vendas
   simultâneas do último item em estoque podem as duas "ver" saldo
   suficiente e as duas venderem — estoque negativo. Não impede, sozinho,
   que um bug de aplicação (ex.: um código que ignore o service e insira
   `ProdutoEstado` direto) corrompa a cadeia.
3. **Índice único parcial no banco** — `uk_produto_estado_aberto
   (produto_id) WHERE data_fim IS NULL` — impede fisicamente que exista mais
   de um estado aberto para o mesmo produto, não importa por onde a escrita
   chegou. É essa camada que converte o invariante de "nós prometemos que só
   vai ter um estado aberto" em "o banco recusa a segunda linha aberta",
   mesmo se as camadas 1 e 2 falharem (bug futuro, migration malfeita, acesso
   direto ao banco, etc.). Ela é a única camada que sobrevive a um erro de
   programação nas outras duas.

Uma prova de integração real (Postgres via Testcontainers, threads reais)
de que a camada 2 realmente faz alguma coisa vive em
`ProdutoEstadoConcorrenciaTest`: duas threads em transações PRÓPRIAS
disputando o mesmo produto — sem a trava, um delta concorrente se perde ou
uma venda a descoberto passa; com a trava, nem uma coisa nem outra
acontece. Ver o arquivo para o mecanismo exato (`TransactionTemplate` com
`PROPAGATION_REQUIRES_NEW` por thread — é fácil escrever um teste de
concorrência que "passa" só porque as duas threads compartilham uma única
transação, e esse teste não prova nada).

## A checagem de centralização (principal)

```bash
cd ~/Git/pib/storehouse-api
rg -n 'ProdutoEstado(' -F --glob 'src/main/**/*.kt' | grep -v 'ProdutoEstadoService.kt' | grep -v 'entities/ProdutoEstado.kt' | grep -v import
```

Saída esperada: **vazia**. Qualquer linha nessa saída é um call site novo
construindo `ProdutoEstado` fora do dono único — investigar antes de
prosseguir (`entities/ProdutoEstado.kt` é excluído porque é a própria
definição da classe; `import` é excluído porque não é uma construção).

## A checagem antiga (rede secundária)

```bash
rg -n -P --glob 'src/main/**/*.kt' '\.estoque\s*(\+=|-=|(?<![=!<>])=(?!=))'
```

Essa era a checagem original, de quando a violação conhecida
(`cancelarVenda` mutando `estadoAtual.estoque` em lugar) ainda existia.
**Hoje ela também está vazia** — a violação foi corrigida, não só
escondida. Mantida como rede secundária: se algum código novo voltar a
mutar `.estoque` em vez de passar por `ProdutoEstadoService`, essa checagem
pega mesmo que a checagem de centralização (que procura por
`ProdutoEstado(`, não por `.estoque`) não cubra o caso.

## A restrição do `saveAndFlush` em `transicionar`

`ProdutoEstadoService.transicionar` fecha o estado atual com
`produtoEstadoRepository.saveAndFlush(atual)` — **não** `save(atual)`. Isso
não é estilo, é obrigatório: o `ActionQueue` do Hibernate ordena TODOS os
INSERTs antes de TODOS os UPDATEs dentro de um mesmo flush. Um `save()`
comum aqui deixaria o INSERT do estado novo (`data_fim = NULL`) chegar ao
banco ANTES do UPDATE que fecha o estado atual — as duas linhas ficariam
com `data_fim IS NULL` ao mesmo tempo, mesmo que só por um instante dentro
da mesma transação. Isso já foi inofensivo (não havia nada que checasse
"só um estado aberto por produto"), mas deixou de ser desde que
`uk_produto_estado_aberto` existe: um índice único PARCIAL não pode ser
`DEFERRABLE`, então ele é verificado no INSERT, dentro da própria
transação — se o INSERT do estado novo chegar antes do UPDATE que fecha o
antigo, a transação inteira falha com violação de constraint única.
`saveAndFlush` força o fechamento a chegar ao banco antes da abertura do
novo, então nunca há duas linhas abertas simultâneas para o mesmo produto,
nem por um instante.

**Essa é uma pegadinha não óbvia.** Alguém, em algum momento, vai olhar
para `saveAndFlush(atual)` sem esse contexto e "simplificar" de volta para
`save(atual)` — um flush explícito parece redundante quando a transação vai
commitar de qualquer forma. Não simplificar: `ProdutoEstadoServiceIntegrationTest`
tem uma prova de integração real dedicada a isso (rodando contra Postgres,
não mock) que falha com `duplicate key value violates unique constraint
"uk_produto_estado_aberto"` se `saveAndFlush` virar `save`.

## A regra de ordenação de lock

Quando uma transação precisa travar VÁRIOS produtos (ex.: uma venda com
múltiplos itens), ela tem que travar sempre na MESMA ordem — hoje,
ordenando por `produtoId` (`sortedBy { it.produto.id }`, em
`VendaService.registrarVenda` e `VendaService.cancelarVenda`, antes de
chamar `ProdutoEstadoService.aplicarDelta` por item). Sem isso: duas vendas
concorrentes que compartilham os mesmos dois produtos, mas os travam em
ordens OPOSTAS, formam um deadlock clássico — a transação A trava
produtoX e espera produtoY; a transação B trava produtoY e espera
produtoX; nenhuma das duas consegue prosseguir. O Postgres detecta esse
ciclo (por padrão, depois de ~1s) e aborta uma das duas transações com
"deadlock detected" — não trava o banco para sempre, mas derruba a venda
de um cliente sem motivo aparente, de um jeito que só aparece sob carga
concorrente real.

`ProdutoEstadoConcorrenciaTest` prova isso também: duas vendas
concorrentes com os mesmos dois produtos, em ordem oposta de item na
requisição, com `assertTimeoutPreemptively` para que um deadlock real falhe
o teste em vez de pendurar o CI. Removendo o `sortedBy`, o teste falha
(ou trava) de forma consistente; um teste de deadlock que não falhe sem a
proteção não prova nada.

## O que quebra se violar

Mutar o estado em lugar apaga o histórico temporal: não dá mais para saber
qual era o estoque/preço/custo em um instante passado, o que quebra
relatórios que recalculam `precoCusto`/`estoque` histórico a partir da
cadeia de `ProdutoEstado` (ex.: `toResponse(relatorio = true)` em
`VendaService.kt`, que lê `it.produto.estadoAtual!!.estoque` esperando que
esse valor reflita a cadeia correta, não um remendo).
