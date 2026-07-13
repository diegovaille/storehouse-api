package br.com.storehouse.service

import br.com.storehouse.config.LiquibaseTestRunner
import br.com.storehouse.config.PostgresTestContainer
import br.com.storehouse.config.TestApplication
import br.com.storehouse.data.entities.Produto
import br.com.storehouse.data.entities.Usuario
import br.com.storehouse.data.model.ItemVendaRequest
import br.com.storehouse.data.model.PagamentoVendaRequest
import br.com.storehouse.data.model.VendaRequest
import br.com.storehouse.data.repository.FilialRepository
import br.com.storehouse.data.repository.ProdutoRepository
import br.com.storehouse.data.repository.TipoProdutoRepository
import br.com.storehouse.data.repository.UsuarioRepository
import br.com.storehouse.exceptions.EstadoInvalidoException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.Duration
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Prova de concorrência real (Postgres via Testcontainers, threads reais — não mocks) de que
 * o lock pessimista de `ProdutoEstadoService` (SELECT ... FOR UPDATE via
 * `ProdutoRepository.findByIdForUpdate`) e o `sortedBy { it.produto.id }` de
 * `VendaService.registrarVenda`/`cancelarVenda` fazem alguma coisa.
 *
 * Uma trava sem teste que falhe na ausência dela é decoração — por isso CADA thread aqui
 * roda numa transação PRÓPRIA (via `TransactionTemplate` com `PROPAGATION_REQUIRES_NEW`,
 * já que `ProdutoEstadoService` usa `Propagation.MANDATORY` e não abre transação sozinho).
 * Se as duas threads compartilhassem uma transação, não haveria duas conexões concorrentes
 * disputando a mesma linha, e o teste passaria mesmo sem o lock — não provaria nada.
 */
@SpringBootTest(
    classes = [TestApplication::class, LiquibaseTestRunner::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgresTestContainer.Initializer::class])
class ProdutoEstadoConcorrenciaTest @Autowired constructor(
    private val produtoEstadoService: ProdutoEstadoService,
    private val vendaService: VendaService,
    private val produtoRepository: ProdutoRepository,
    private val filialRepository: FilialRepository,
    private val tipoProdutoRepository: TipoProdutoRepository,
    private val usuarioRepository: UsuarioRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val transactionManager: PlatformTransactionManager
) {

    // --- infraestrutura de setup: mesmo padrão de ProdutoEstadoServiceIntegrationTest ---

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

        val produto = Produto(codigoBarras = codigoBarras, nome = "Produto Concorrencia $codigoBarras", tipo = tipo, filial = filial)
        return produtoRepository.save(produto)
    }

    private fun usuarioPersistido(email: String): Usuario =
        usuarioRepository.findByEmail(email)
            ?: usuarioRepository.save(Usuario(email = email, username = email))

    /**
     * Roda `acao` numa transação NOVA e independente, via o mesmo `PlatformTransactionManager`
     * que os beans `@Transactional` do app usam. É isso que faz `ProdutoEstadoService`
     * (`Propagation.MANDATORY`) aceitar a chamada, e é isso que garante que cada thread do
     * teste segura sua própria conexão/transação — pré-requisito para o SELECT ... FOR UPDATE
     * de uma thread realmente bloquear a outra.
     */
    private fun <T : Any> emTransacaoNova(acao: () -> T): T {
        val template = TransactionTemplate(transactionManager)
        template.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        return template.execute { acao() }!!
    }

    private fun estoqueAbertoDe(produtoId: UUID): Int =
        jdbcTemplate.queryForObject(
            "select estoque from produto_estado where produto_id = ? and data_fim is null",
            Int::class.java,
            produtoId
        )!!

    private sealed class ResultadoDelta
    private data class Sucesso(val estoque: Int) : ResultadoDelta()
    private data class Falha(val erro: Throwable) : ResultadoDelta()

    /**
     * Dispara duas tarefas em threads reais de um pool de 2, sincronizadas por dois latches:
     * `pronto` garante que as duas já estão rodando (e portanto prestes a competir pelo lock)
     * antes de `partida` liberar as duas ao mesmo tempo — maximiza a sobreposição real das
     * duas transações concorrentes, em vez de deixar ao acaso a ordem de agendamento do SO.
     */
    private fun <T> dispararConcorrente(tarefaA: () -> T, tarefaB: () -> T): Pair<T, T> {
        val executor = Executors.newFixedThreadPool(2)
        val pronto = CountDownLatch(2)
        val partida = CountDownLatch(1)

        fun envolver(tarefa: () -> T) = Callable<T> {
            pronto.countDown()
            partida.await()
            tarefa()
        }

        try {
            val futuroA = executor.submit(envolver(tarefaA))
            val futuroB = executor.submit(envolver(tarefaB))

            assertTrue(pronto.await(5, TimeUnit.SECONDS), "as duas threads deveriam estar prontas em 5s")
            partida.countDown()

            val resultadoA = futuroA.get(15, TimeUnit.SECONDS)
            val resultadoB = futuroB.get(15, TimeUnit.SECONDS)
            return resultadoA to resultadoB
        } finally {
            executor.shutdown()
        }
    }

    /**
     * TESTE 1 — delta concorrente não pode se perder.
     *
     * Uma única rodada dispara a corrida no MÁXIMO uma vez: as duas threads têm que se
     * sobrepor exatamente na janela leitura-então-escrita para o update perdido acontecer, e
     * isso é uma moeda — medido empiricamente em 1 de 4 execuções. Um teste que só detecta a
     * ausência do lock 25% das vezes é pior que inútil (falsa confiança), então aqui rodamos
     * `RODADAS` vezes a corrida (+5, -3) contra o MESMO produto e conferimos o total
     * acumulado: com o lock, toda rodada é correta e o total bate certinho; sem o lock,
     * basta UMA rodada perder uma escrita para o total dar errado. 20 rodadas de 2 threads
     * levam poucos segundos e, na prática, derrubam a taxa de detecção para efetivamente 100%
     * (provado abaixo rodando a suíte sem o lock).
     */
    @Test
    fun `delta concorrente nao se perde`() {
        val produto = produtoPersistido("conc-delta-soma")
        emTransacaoNova {
            produtoEstadoService.criarInicial(produto, estoque = 10, preco = BigDecimal("10.00"), precoCusto = BigDecimal("5.00"))
        }

        fun aplicar(delta: Int): ResultadoDelta =
            try {
                Sucesso(emTransacaoNova { produtoEstadoService.aplicarDelta(produto.id, delta) }.estoque)
            } catch (e: Throwable) {
                Falha(e)
            }

        val rodadas = 20
        for (rodada in 1..rodadas) {
            val (resultadoA, resultadoB) = dispararConcorrente({ aplicar(5) }, { aplicar(-3) })

            assertTrue(resultadoA is Sucesso, "rodada $rodada: thread A (+5) deveria ter sucesso, foi $resultadoA")
            assertTrue(resultadoB is Sucesso, "rodada $rodada: thread B (-3) deveria ter sucesso, foi $resultadoB")
        }

        val esperado = 10 + rodadas * 2
        assertEquals(
            esperado,
            estoqueAbertoDe(produto.id),
            "delta concorrente perdido em alguma das $rodadas rodadas: esperava 10 + $rodadas*(+5-3) = $esperado"
        )
    }

    /**
     * TESTE 2 — O MAIS IMPORTANTE. Não se pode vender a descoberto.
     *
     * Mesmo problema do teste 1: uma corrida disparada uma única vez só pega a janela de
     * overlap por sorte — medido empiricamente em 2 de 4 execuções sem o lock. Aqui cada
     * rodada usa um produto FRESCO (estoque = 1) para não acumular estado entre rodadas, e
     * a asserção (exatamente 1 sucesso, exatamente 1 `EstadoInvalidoException`, estoque final
     * 0) roda a CADA rodada — a primeira rodada que quebrar (oversell -1 ou update perdido 1)
     * falha o teste imediatamente, nomeando a rodada.
     */
    @Test
    fun `nao permite vender a descoberto em duas threads simultaneas`() {
        fun aplicar(produtoId: UUID): ResultadoDelta =
            try {
                Sucesso(emTransacaoNova { produtoEstadoService.aplicarDelta(produtoId, -1) }.estoque)
            } catch (e: Throwable) {
                Falha(e)
            }

        val rodadas = 20
        for (rodada in 1..rodadas) {
            val produto = produtoPersistido("conc-oversell-$rodada")
            emTransacaoNova {
                produtoEstadoService.criarInicial(produto, estoque = 1, preco = BigDecimal("10.00"), precoCusto = BigDecimal("5.00"))
            }

            val (resultadoA, resultadoB) = dispararConcorrente({ aplicar(produto.id) }, { aplicar(produto.id) })
            val resultados = listOf(resultadoA, resultadoB)

            val sucessos = resultados.filterIsInstance<Sucesso>()
            val falhas = resultados.filterIsInstance<Falha>()

            assertEquals(
                1, sucessos.size,
                "rodada $rodada: exatamente uma das duas threads deveria ter sucesso: $resultados"
            )
            assertEquals(
                1, falhas.size,
                "rodada $rodada: exatamente uma das duas threads deveria falhar: $resultados"
            )
            assertTrue(
                falhas.single().erro is EstadoInvalidoException,
                "rodada $rodada: a falha deveria ser EstadoInvalidoException (estoque insuficiente), foi ${falhas.single().erro}"
            )

            assertEquals(
                0, estoqueAbertoDe(produto.id),
                "rodada $rodada: nunca pode sobrar -1 (venda a descoberto) nem 1 (update perdido)"
            )
        }
    }

    /**
     * TESTE 3 — duas vendas concorrentes com os mesmos dois produtos, em ordem OPOSTA, não
     * podem dar deadlock.
     *
     * `VendaService.registrarVenda` ordena os itens por `produto.id` antes de aplicar os
     * deltas (e portanto antes de travar as linhas), exatamente para que duas vendas
     * concorrentes que seguram os mesmos produtos travem sempre na mesma ordem. Aqui o pedido
     * de entrada é deliberadamente oposto entre as duas vendas — é o `sortedBy` dentro do
     * service que precisa normalizar a ordem de travamento; sem ele, a thread A trava
     * produtoA→produtoB e a thread B trava produtoB→produtoA ao mesmo tempo: deadlock
     * clássico. `assertTimeoutPreemptively` garante que, se isso travar de verdade, o teste
     * falha em vez de pendurar o CI para sempre.
     */
    @Test
    fun `duas vendas concorrentes com produtos em ordem oposta nao dao deadlock`() {
        val produtoA = produtoPersistido("conc-venda-produto-a")
        val produtoB = produtoPersistido("conc-venda-produto-b")
        emTransacaoNova {
            produtoEstadoService.criarInicial(produtoA, estoque = 10, preco = BigDecimal("10.00"), precoCusto = BigDecimal("5.00"))
        }
        emTransacaoNova {
            produtoEstadoService.criarInicial(produtoB, estoque = 10, preco = BigDecimal("20.00"), precoCusto = BigDecimal("8.00"))
        }
        val usuario = usuarioPersistido("conc-venda@teste.storehouse")
        val filial = filialRepository.findById(FILIAL_SEED_ID).orElseThrow()

        fun requisicaoNaOrdem(vararg codigos: String) = VendaRequest(
            itens = codigos.map { ItemVendaRequest(codigoBarras = it, quantidade = 1) },
            pagamentos = listOf(PagamentoVendaRequest(tipo = "DINHEIRO", valor = BigDecimal("1000.00")))
        )

        fun vender(request: VendaRequest): Throwable? =
            try {
                vendaService.registrarVenda(filial.id, request, usuario.email)
                null
            } catch (e: Throwable) {
                e
            }

        assertTimeoutPreemptively(
            Duration.ofSeconds(25),
            {
                val (erroA, erroB) = dispararConcorrente(
                    { vender(requisicaoNaOrdem("conc-venda-produto-a", "conc-venda-produto-b")) },
                    { vender(requisicaoNaOrdem("conc-venda-produto-b", "conc-venda-produto-a")) }
                )

                assertNull(erroA, "venda A (ordem a,b) não deveria falhar: $erroA")
                assertNull(erroB, "venda B (ordem b,a) não deveria falhar: $erroB")
            },
            "vendas concorrentes com produtos em ordem oposta travaram (deadlock)"
        )

        // Estoque inicial 10, duas vendas de 1 unidade cada (uma por thread): 10 - 1 - 1 = 8.
        assertEquals(8, estoqueAbertoDe(produtoA.id), "as duas vendas deveriam ter debitado 1 unidade cada de produtoA")
        assertEquals(8, estoqueAbertoDe(produtoB.id), "as duas vendas deveriam ter debitado 1 unidade cada de produtoB")
    }

    companion object {
        private val ORGANIZACAO_SEED_ID = UUID.fromString("20587698-1b67-4cbb-8a08-d7e9fe56a77d")
        private val FILIAL_SEED_ID = UUID.fromString("e741e0b4-02f9-4e6e-b3c3-4318d36477b3")
        private val TIPO_LIVRO_SEED_ID = UUID.fromString("1b4b2f66-6b55-4ac8-90b0-027fb7d9c1fe")
    }
}
