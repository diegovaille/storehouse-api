---
name: liquibase-migrations
description: Use quando criar migration, changelog, alterar schema, adicionar coluna ou tabela — criar o arquivo de changelog não basta, precisa ser incluído no db.changelog-master.xml ou o boot quebra.
---

## A regra

Toda migration nova é um arquivo
`db.changelog-<major>.<minor>-<slug>.xml` dentro de
`src/main/resources/db/changelog/`. Criar o arquivo **não basta** — ele
precisa ser adicionado como `<include>` em `db.changelog-master.xml`, na
ordem em que deve rodar. Migrations rodam no boot da aplicação; com
`spring.jpa.hibernate.ddl-auto: validate`, se a entidade JPA divergir do
schema real (porque a migration que criaria a coluna/tabela não rodou), o
boot quebra imediatamente.

## A checagem

```bash
cd ~/Git/pib/storehouse-api/src/main/resources/db/changelog
for f in db.changelog-[0-9]*.xml; do
  grep -q "$f" db.changelog-master.xml || echo "ORFAO (nao incluido no master): $f"
done
```

Saída esperada hoje: vazia. Qualquer linha impressa é um changelog órfão —
existe no disco mas não vai rodar, e a entidade JPA correspondente vai
divergir do schema no próximo boot.

## O que quebra se violar

Um changelog órfão nunca é aplicado pelo Liquibase. Se a entidade JPA já
foi atualizada para refletir a mudança de schema pretendida (nova coluna,
nova tabela, novo índice), `ddl-auto: validate` derruba o boot da aplicação
no próximo deploy — não é um erro silencioso, é uma falha de inicialização
que bloqueia o release inteiro.

## Violação conhecida

Nenhuma hoje — a checagem está limpa. Se aparecer uma linha nesta checagem,
é uma regressão real introduzida na sessão atual, não um bug pré-existente
documentado; corrigir imediatamente (adicionar o `<include>` faltante em
`db.changelog-master.xml`), não adiar.

## Notas

- Não há exclusões nesta checagem: todo arquivo que casa com
  `db.changelog-[0-9]*.xml` é, por definição, uma migration versionada que
  precisa estar no master. `db.changelog-master.xml` em si não casa com o
  glob (não começa com dígito depois do prefixo), então não se auto-verifica.
- A ordem dos `<include>` no master importa — migrations rodam na ordem
  declarada, não em ordem alfabética do filesystem. Ao adicionar uma nova,
  inserir na posição correspondente à numeração `<major>.<minor>`.
