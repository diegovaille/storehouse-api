-- ============================================
-- SOLUÇÃO: Erro de Checksum do Liquibase
-- ============================================
-- Erro: db.changelog-3.6-parametros-calculo.xml checksum changed
--
-- Execute este SQL no seu banco PostgreSQL:

UPDATE databasechangelog
SET md5sum = NULL
WHERE id = 'create-parametro-calculo-table'
  AND author = 'diegovaille';

-- Pronto! Agora rode a aplicação normalmente.
-- O Liquibase vai recalcular o checksum automaticamente.


