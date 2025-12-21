# Solução para Erro de Checksum do Liquibase

## 🚀 SOLUÇÃO RÁPIDA (Execute este SQL):

```sql
-- Limpar o checksum para o Liquibase recalcular
UPDATE databasechangelog 
SET md5sum = NULL 
WHERE id = 'create-parametro-calculo-table' 
  AND author = 'diegovaille';
```

**Depois execute a aplicação normalmente.**

---

# Solução Detalhada para Erro de Checksum do Liquibase

## 🐛 Problema

```
liquibase.exception.ValidationFailedException: Validation Failed:
     1 changesets check sum
          db/changelog/db.changelog-3.6-parametros-calculo.xml::create-parametro-calculo-table::diegovaille 
          was: 9:c57097610494c9b94d653bae5f5dd29a 
          but is now: 9:dc88b783b0b4fd3a3ac5ea3041510f86
```

**Causa:** O changelog 3.6 foi alterado (DECIMAL → DOUBLE PRECISION) após já ter sido executado no banco.

## ✅ Solução Aplicada

Criamos uma estratégia de 3 passos:

### 1. Reverter Changelog 3.6 ao Original
**Arquivo:** `db.changelog-3.6-parametros-calculo.xml`

Mantido com o tipo original para preservar o checksum:
```xml
<column name="valor" type="DECIMAL(10,2)">
    <constraints nullable="false"/>
</column>
```

### 2. Criar Novo Changelog 3.7 para Corrigir o Tipo
**Arquivo:** `db.changelog-3.7-fix-parametro-tipo.xml` (NOVO)

```xml
<changeSet id="fix-parametro-calculo-valor-type" author="diegovaille">
    <preConditions onFail="MARK_RAN">
        <columnExists tableName="parametro_calculo" columnName="valor" schemaName="pinguim"/>
    </preConditions>
    
    <modifyDataType 
        tableName="parametro_calculo" 
        schemaName="pinguim"
        columnName="valor" 
        newDataType="DOUBLE PRECISION"/>
</changeSet>
```

**Benefícios:**
- ✅ Não altera changelog existente (sem conflito de checksum)
- ✅ `preConditions` garante que só executa se a tabela existir
- ✅ `onFail="MARK_RAN"` marca como executado se falhar
- ✅ Altera o tipo da coluna automaticamente

### 3. Atualizar Master Changelog
```xml
<include file="db/changelog/db.changelog-3.6-parametros-calculo.xml"/>
<include file="db/changelog/db.changelog-3.7-fix-parametro-tipo.xml"/>
```

## 🔄 Como Funciona

**Primeira execução (banco novo):**
1. Liquibase executa 3.6 → cria tabela com `DECIMAL(10,2)`
2. Liquibase executa 3.7 → altera para `DOUBLE PRECISION`
3. Resultado: tabela com tipo correto ✅

**Execuções subsequentes:**
1. Liquibase pula 3.6 (já executado, checksum OK)
2. Liquibase executa 3.7 uma vez
3. Liquibase pula 3.7 nas próximas (já executado)

**Banco que já tinha executado 3.6 (seu caso):**
1. Liquibase valida 3.6 → checksum OK (voltamos ao original)
2. Liquibase executa 3.7 → altera tipo para `DOUBLE PRECISION`
3. Problema resolvido! ✅

## 📋 Alternativa Manual (se necessário)

Se ainda der erro, execute manualmente no PostgreSQL:

```sql
-- Opção 1: Atualizar checksum no Liquibase
UPDATE databasechangelog 
SET md5sum = NULL 
WHERE id = 'create-parametro-calculo-table' 
  AND author = 'diegovaille';

-- Opção 2: Alterar tipo manualmente e marcar como executado
ALTER TABLE pinguim.parametro_calculo 
ALTER COLUMN valor TYPE DOUBLE PRECISION;

-- Depois rodar a aplicação
```

Arquivo SQL criado: `fix-liquibase-checksum.sql`

## ✅ Status

- ✅ Changelog 3.6 revertido ao original (DECIMAL)
- ✅ Changelog 3.7 criado (modifyDataType → DOUBLE PRECISION)
- ✅ Master changelog atualizado
- ✅ Entidade Kotlin usa `Double` (correto)
- ✅ OpenAPI spec usa `double` (correto)

## 🚀 Próximos Passos

1. Rodar a aplicação: `./gradlew bootRun`
2. Liquibase vai executar o 3.7 automaticamente
3. Tipo da coluna será corrigido para `DOUBLE PRECISION`
4. Aplicação deve iniciar normalmente

**A solução está pronta! Tente rodar a aplicação novamente.** 🎉

