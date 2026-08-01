package br.com.storehouse.api.controller

import br.com.storehouse.config.LiquibaseTestRunner
import br.com.storehouse.config.SecurityTestPostgresContainer
import br.com.storehouse.config.TestApplication
import br.com.storehouse.data.entities.Produto
import br.com.storehouse.data.repository.FilialRepository
import br.com.storehouse.data.repository.ProdutoRepository
import br.com.storehouse.data.repository.TipoProdutoRepository
import br.com.storehouse.service.ProdutoEstadoService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

/**
 * Prova, através da pilha REAL de Spring Security (`SecurityConfig` de produção, não uma
 * cópia nem um standalone MockMvc que a ignora), as duas regras da issue #17:
 *
 * 1. `GET /api/publico/catalogo/{filialId}` funciona SEM Authorization header — 200, não
 *    401 nem 500 — e o corpo NUNCA contém `precoCusto` nem estoque numérico.
 * 2. `GET /api/produtos` (endpoint de operação, preço de custo + estoque exato) volta a
 *    exigir autenticação — 401 sem token — fechando a armadilha original da #17 (rota
 *    pública que estourava 500 porque o controller exigia `@AuthenticationPrincipal`).
 *
 * Por quê um profile próprio ("sectest") em vez de "test": `SecurityConfig` é
 * `@Profile("!test")` — sob o profile "test" (usado pelo resto da suíte de integração) ele
 * fica DESLIGADO de propósito, e um MockMvc montado sobre esse contexto nunca veria as
 * regras de `authorizeHttpRequests`. "sectest" satisfaz "!test", então este teste roda com
 * o SecurityFilterChain de produção de verdade — exatamente o requisito da issue: "a única
 * forma de saber é exercitar sem auth através da cadeia real do Spring".
 */
@SpringBootTest(
    classes = [TestApplication::class, LiquibaseTestRunner::class],
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@ActiveProfiles("sectest")
@ContextConfiguration(initializers = [SecurityTestPostgresContainer.Initializer::class])
class CatalogoPublicoControllerSecurityTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val produtoRepository: ProdutoRepository,
    private val filialRepository: FilialRepository,
    private val tipoProdutoRepository: TipoProdutoRepository,
    private val produtoEstadoService: ProdutoEstadoService,
    private val jdbcTemplate: JdbcTemplate
) {

    companion object {
        // Mesmo seed de 1.6-dados-iniciais.xml usado por ProdutoEstadoServiceIntegrationTest.
        private val ORGANIZACAO_SEED_ID = UUID.fromString("20587698-1b67-4cbb-8a08-d7e9fe56a77d")
        private val FILIAL_SEED_ID = UUID.fromString("e741e0b4-02f9-4e6e-b3c3-4318d36477b3")
        private val TIPO_LIVRO_SEED_ID = UUID.fromString("1b4b2f66-6b55-4ac8-90b0-027fb7d9c1fe")
    }

    /**
     * Este teste roda num Spring context próprio (profile "sectest", distinto do "test"
     * usado pelo resto da suíte), mas o mesmo container Postgres físico (o `object`
     * PostgresTestContainer é um singleton por JVM). LiquibaseTestRunner desta suíte roda
     * com `isDropFirst = true` na primeira vez que ESTE contexto sobe, então o seed de 1.6
     * já está lá — mas reinserir aqui (idempotente, ON CONFLICT DO NOTHING) evita depender
     * da ordem entre suítes, mesma razão documentada em
     * ProdutoEstadoServiceIntegrationTest.garantirSeedDe16().
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

    private fun produtoComEstoque(codigoBarras: String, nome: String, estoque: Int, preco: String, precoCusto: String): Produto {
        garantirSeedDe16()

        val filial = filialRepository.findById(FILIAL_SEED_ID)
            .orElseThrow { IllegalStateException("Filial seed (1.6) não encontrada") }
        val tipo = tipoProdutoRepository.findById(TIPO_LIVRO_SEED_ID)
            .orElseThrow { IllegalStateException("TipoProduto 'Livro' seed (1.6) não encontrado") }

        val produto = produtoRepository.save(
            Produto(codigoBarras = codigoBarras, nome = nome, tipo = tipo, filial = filial, imagemUrl = "https://cdn.example/$codigoBarras.jpg")
        )
        produtoEstadoService.criarInicial(
            produto,
            estoque = estoque,
            preco = BigDecimal(preco),
            precoCusto = BigDecimal(precoCusto)
        )
        return produto
    }

    @Test
    @Transactional
    fun `catalogo publico responde 200 sem Authorization header, nao 401 nem 500`() {
        produtoComEstoque("sec-cat-001", "Bíblia de Estudo", estoque = 10, preco = "89.90", precoCusto = "40.00")

        mockMvc.perform(get("/api/publico/catalogo/$FILIAL_SEED_ID"))
            .andExpect(status().isOk)
    }

    @Test
    @Transactional
    fun `corpo do catalogo publico nao contem precoCusto nem estoque numerico`() {
        produtoComEstoque("sec-cat-002", "Camiseta Primeira", estoque = 5, preco = "59.90", precoCusto = "20.00")

        val corpo = mockMvc.perform(get("/api/publico/catalogo/$FILIAL_SEED_ID"))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        // Assertiva crítica de segurança: se um refactor futuro trocar o mapeamento por
        // ProdutoResponse (ou simplesmente adicionar o campo de volta ao DTO de vitrine),
        // este teste precisa quebrar. Ver nota de mutation-check no corpo do PR.
        assertFalse(corpo.contains("precoCusto"), "vitrine pública vazou precoCusto: $corpo")
        assertFalse(corpo.contains("\"estoque\""), "vitrine pública vazou estoque numérico: $corpo")
        assertTrue(corpo.contains("disponivel"), "vitrine pública deveria expor 'disponivel': $corpo")
        assertTrue(corpo.contains("Camiseta Primeira"))
    }

    @Test
    @Transactional
    fun `disponivel e true para produto com estoque e false para produto zerado`() {
        produtoComEstoque("sec-cat-003", "Caneca Com Estoque", estoque = 3, preco = "35.00", precoCusto = "12.00")
        produtoComEstoque("sec-cat-004", "Caneca Esgotada", estoque = 0, preco = "35.00", precoCusto = "12.00")

        val corpo = mockMvc.perform(get("/api/publico/catalogo/$FILIAL_SEED_ID"))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        val comEstoque = extrairItem(corpo, "Caneca Com Estoque")
        val esgotada = extrairItem(corpo, "Caneca Esgotada")

        assertTrue(comEstoque.contains("\"disponivel\":true"), "esperava disponivel=true: $comEstoque")
        assertTrue(esgotada.contains("\"disponivel\":false"), "esperava disponivel=false: $esgotada")
    }

    @Test
    fun `catalogo publico de filial inexistente responde 200 com lista vazia`() {
        val filialInexistente = UUID.randomUUID()

        mockMvc.perform(get("/api/publico/catalogo/$filialInexistente"))
            .andExpect(status().isOk)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().json("[]"))
    }

    @Test
    fun `GET api-produtos (endpoint interno) exige autenticacao — 401 sem token`() {
        mockMvc.perform(get("/api/produtos"))
            .andExpect(status().isUnauthorized)
    }

    /** Extrai o objeto JSON (representação em linha única) do item cujo "nome" é passado. */
    private fun extrairItem(corpoJsonArray: String, nome: String): String {
        val itens = corpoJsonArray.trim().removePrefix("[").removeSuffix("]").split("},{")
        return itens.map { if (!it.startsWith("{")) "{$it" else it }
            .map { if (!it.endsWith("}")) "$it}" else it }
            .first { it.contains("\"$nome\"") }
    }
}
