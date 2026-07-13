package br.com.storehouse.service

import br.com.storehouse.config.LiquibaseTestRunner
import br.com.storehouse.config.PostgresTestContainer
import br.com.storehouse.config.TestApplication
import br.com.storehouse.data.entities.Produto
import br.com.storehouse.data.repository.FilialRepository
import br.com.storehouse.data.repository.ProdutoRepository
import br.com.storehouse.data.repository.TipoProdutoRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

/**
 * Prova de integração (Fase C, achado CRÍTICO) para `ProdutoEstadoService.transicionar`.
 *
 * O bug: Hibernate ordena todos os INSERTs antes de todos os UPDATEs no flush do
 * ActionQueue. Sem `saveAndFlush` no fechamento do estado atual, o INSERT do estado novo
 * (data_fim = NULL) seria enviado ao banco ANTES do UPDATE que fecha o estado antigo — as
 * duas linhas ficariam com data_fim NULL ao mesmo tempo, ainda que por um instante dentro da
 * mesma transação.
 *
 * Isso deixou de ser inofensivo: a migration `db.changelog-4.5-estoque-estado-unico.xml`
 * adiciona `uk_produto_estado_aberto`, um índice único parcial em
 * `produto_estado (produto_id) WHERE data_fim IS NULL`. Um índice parcial não pode ser
 * DEFERRABLE, então ele é verificado no INSERT, dentro da própria transação — se o INSERT do
 * estado novo chegar ao banco antes do UPDATE que fecha o antigo, a transação inteira falha
 * com violação de constraint única.
 *
 * `aplicarDelta deixa exatamente um estado aberto no banco para o produto` é, por isso, uma
 * prova real (não apenas uma inspeção de string de SQL): ela SÓ passa se `transicionar` de
 * fato fechar o estado atual no banco (saveAndFlush) antes de abrir o novo. Revertendo
 * `saveAndFlush` para `save`, o teste falha com
 * `ERROR: duplicate key value violates unique constraint "uk_produto_estado_aberto"`.
 */
@SpringBootTest(
    classes = [TestApplication::class, LiquibaseTestRunner::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgresTestContainer.Initializer::class])
class ProdutoEstadoServiceIntegrationTest @Autowired constructor(
    private val produtoEstadoService: ProdutoEstadoService,
    private val produtoRepository: ProdutoRepository,
    private val filialRepository: FilialRepository,
    private val tipoProdutoRepository: TipoProdutoRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val entityManager: EntityManager
) {

    /**
     * TipoProduto.campos é `@Column(columnDefinition = "jsonb")` mapeado como String; o
     * Hibernate 6 envia esse valor como varchar, que o Postgres rejeita contra uma coluna
     * jsonb ("column \"campos\" is of type jsonb but expression is of type character
     * varying"). Em produção isso nunca acontece porque tipos são sempre seedados via
     * Liquibase (INSERT puro), nunca criados via JPA — então em vez de contornar a entidade
     * (fora de escopo), o teste carrega filial/tipo já seedados por
     * db.changelog-1.6-dados-iniciais.xml e só persiste o Produto via JPA, que não tem
     * coluna jsonb.
     *
     * `garantirSeedDe16()` reinsere esses três registros (idempotente, via
     * `ON CONFLICT DO NOTHING`) antes de lê-los: esta suíte roda no MESMO Spring context —
     * e portanto no MESMO banco do Testcontainer — que `RunCucumberTest`, cujo
     * `CommonSteps.limparBanco()` (hook `@Before`, roda antes de CADA cenário) faz
     * `filialRepository.deleteAll()` / `organizacaoRepository.deleteAll()` /
     * `tipoProdutoRepository.deleteAll()` (transitivamente, via cascata de FK) sem nunca
     * re-rodar o Liquibase depois. Como o Gradle roda os dois engines (JUnit5 e Cucumber) na
     * MESMA JVM/worker e o contexto Spring é cacheado pela assinatura de configuração (que é
     * idêntica entre esta classe e `CucumberSpringConfig`), a ORDEM entre as suítes não é
     * garantida — sem essa reinserção, este teste passa ou falha dependendo de quem rodou
     * por último. Verificado empiricamente: com a leitura pura (sem reinserção), a suíte
     * completa falhava com "Filial seed (1.6) não encontrada" sempre que o Cucumber corria
     * antes.
     */
    private fun garantirSeedDe16() {
        jdbcTemplate.update(
            """
            INSERT INTO organizacao (id, nome, cnpj, razao_social, endereco, municipio, estado, tipo)
            VALUES (?, 'Primeira Igreja Batista', '52579836000196',
                    'Primeira Igreja Batista em Mogi das Cruzes', 'Rua Barao de Jaceguai, 1019, Centro',
                    'Mogi das Cruzes', 'SP', 'IGREJA')
            ON CONFLICT (id) DO NOTHING
            """.trimIndent(),
            ORGANIZACAO_SEED_ID
        )
        jdbcTemplate.update(
            """
            INSERT INTO filial (id, organizacao_id, nome, cnpj, razao_social, municipio, estado, ativo)
            VALUES (?, ?, 'Primeira Store', '52579836000196', 'Primeira Store - Loja Oficial',
                    'Mogi das Cruzes', 'SP', true)
            ON CONFLICT (id) DO NOTHING
            """.trimIndent(),
            FILIAL_SEED_ID, ORGANIZACAO_SEED_ID
        )
        jdbcTemplate.update(
            """
            INSERT INTO tipo_produto (id, nome, campos)
            VALUES (?, 'Livro', ?::jsonb)
            ON CONFLICT (id) DO NOTHING
            """.trimIndent(),
            TIPO_LIVRO_SEED_ID, """{"autor":"String","editora":"String","ano":"Int"}"""
        )
    }

    private fun produtoPersistido(codigoBarras: String): Produto {
        garantirSeedDe16()

        val filial = filialRepository.findById(FILIAL_SEED_ID)
            .orElseThrow { IllegalStateException("Filial seed (1.6) não encontrada") }
        val tipo = tipoProdutoRepository.findById(TIPO_LIVRO_SEED_ID)
            .orElseThrow { IllegalStateException("TipoProduto 'Livro' seed (1.6) não encontrado") }

        val produto = Produto(codigoBarras = codigoBarras, nome = "Produto Fase C $codigoBarras", tipo = tipo, filial = filial)
        return produtoRepository.save(produto)
    }

    companion object {
        private val ORGANIZACAO_SEED_ID = UUID.fromString("20587698-1b67-4cbb-8a08-d7e9fe56a77d")
        private val FILIAL_SEED_ID = UUID.fromString("e741e0b4-02f9-4e6e-b3c3-4318d36477b3")
        private val TIPO_LIVRO_SEED_ID = UUID.fromString("1b4b2f66-6b55-4ac8-90b0-027fb7d9c1fe")
    }

    @Test
    @Transactional
    fun `aplicarDelta deixa exatamente um estado aberto no banco para o produto`() {
        val produto = produtoPersistido("fase-c-999")
        produtoEstadoService.criarInicial(
            produto,
            estoque = 10,
            preco = BigDecimal("10.00"),
            precoCusto = BigDecimal("5.00")
        )

        produtoEstadoService.aplicarDelta(produto.id, -3)
        entityManager.flush()

        val abertos = jdbcTemplate.queryForObject(
            "select count(*) from produto_estado where produto_id = ? and data_fim is null",
            Long::class.java,
            produto.id
        )
        assertEquals(1L, abertos)
    }

    /**
     * Prova da migration 4.5, changeset "estoque-estado-unico-limpeza" — task 4.
     *
     * Não dá para recriar o estado sujo (duas linhas abertas para o mesmo produto)
     * diretamente em `produto`/`produto_estado`: a migration já rodou nesta suíte (via
     * LiquibaseTestRunner) e `uk_produto_estado_aberto` rejeitaria o segundo INSERT aberto —
     * exatamente o que o índice existe para impedir. Então esta prova roda a MESMA instrução
     * SQL do changeset de limpeza contra tabelas-escopo de sessão (`CREATE TEMP TABLE`) que
     * replicam só as colunas que a limpeza lê (`produto.estado_atual_id`,
     * `produto_estado.produto_id`, `produto_estado.data_fim`), livres do índice.
     */
    @Test
    @Transactional
    fun `changeset de limpeza da migration 4-5 fecha o estado orfao e preserva o estadoAtual`() {
        jdbcTemplate.execute("CREATE TEMP TABLE scratch_produto (id uuid primary key, estado_atual_id uuid) ON COMMIT DROP")
        jdbcTemplate.execute(
            "CREATE TEMP TABLE scratch_produto_estado (id uuid primary key, produto_id uuid, data_fim timestamp) ON COMMIT DROP"
        )

        val produtoId = UUID.randomUUID()
        val estadoOrfaoId = UUID.randomUUID()
        val estadoAtualId = UUID.randomUUID()

        // Produto aponta estadoAtual para estadoAtualId; estadoOrfaoId é debris de corrida:
        // um segundo estado aberto para o mesmo produto que nunca deveria existir.
        jdbcTemplate.update("insert into scratch_produto (id, estado_atual_id) values (?, ?)", produtoId, estadoAtualId)
        jdbcTemplate.update(
            "insert into scratch_produto_estado (id, produto_id, data_fim) values (?, ?, null)",
            estadoOrfaoId, produtoId
        )
        jdbcTemplate.update(
            "insert into scratch_produto_estado (id, produto_id, data_fim) values (?, ?, null)",
            estadoAtualId, produtoId
        )

        // Mesma instrução SQL do changeset "estoque-estado-unico-limpeza", só com os nomes de
        // tabela trocados para as tabelas-escopo desta transação.
        jdbcTemplate.update(
            """
            UPDATE scratch_produto_estado pe
               SET data_fim = NOW()
              FROM scratch_produto p
             WHERE pe.produto_id = p.id
               AND pe.data_fim IS NULL
               AND (p.estado_atual_id IS NULL OR pe.id <> p.estado_atual_id)
            """.trimIndent()
        )

        val orfaoAindaAberto = jdbcTemplate.queryForObject(
            "select data_fim is null from scratch_produto_estado where id = ?",
            Boolean::class.java,
            estadoOrfaoId
        )
        val atualAindaAberto = jdbcTemplate.queryForObject(
            "select data_fim is null from scratch_produto_estado where id = ?",
            Boolean::class.java,
            estadoAtualId
        )

        assertEquals(false, orfaoAindaAberto, "estado orfao deveria ter sido fechado pela limpeza")
        assertEquals(true, atualAindaAberto, "estadoAtual do produto nao deveria ser tocado pela limpeza")
    }
}
