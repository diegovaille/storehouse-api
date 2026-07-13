package br.com.storehouse.service

import br.com.storehouse.data.entities.Filial
import br.com.storehouse.data.entities.Organizacao
import br.com.storehouse.data.entities.Produto
import br.com.storehouse.data.entities.ProdutoEstado
import br.com.storehouse.data.entities.TipoProduto
import br.com.storehouse.data.repository.ProdutoEstadoRepository
import br.com.storehouse.data.repository.ProdutoRepository
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import br.com.storehouse.exceptions.EstadoInvalidoException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.util.*

class ProdutoEstadoServiceTest {

    private val produtoRepo: ProdutoRepository = Mockito.mock(ProdutoRepository::class.java).also { r ->
        Mockito.`when`(r.save(Mockito.any(Produto::class.java)))
            .thenAnswer { it.arguments[0] as Produto }
    }
    private val produtoEstadoRepo: ProdutoEstadoRepository = Mockito.mock(ProdutoEstadoRepository::class.java).also { r ->
        Mockito.`when`(r.save(Mockito.any(ProdutoEstado::class.java)))
            .thenAnswer { it.arguments[0] as ProdutoEstado }
    }
    private val service = ProdutoEstadoService(produtoRepo, produtoEstadoRepo)

    private fun filial() = Filial(organizacao = Organizacao())

    private fun produto(estoque: Int?, preco: String = "10.00", precoCusto: String = "5.00"): Produto {
        val p = Produto(
            codigoBarras = "789",
            nome = "Camiseta",
            tipo = TipoProduto(),
            filial = filial()
        )
        // service não lê mais `produto.estadoAtual` diretamente para descobrir o estado
        // aberto (ver comentário de `estadoAbertoDe` em ProdutoEstadoService) — ele consulta
        // produtoEstadoRepository.findByProdutoIdAndDataFimIsNull. O mock precisa refletir
        // isso, senão todo teste que dependia do estado aberto vê `null`.
        if (estoque != null) {
            val estado = ProdutoEstado(
                produto = p,
                estoque = estoque,
                preco = BigDecimal(preco),
                precoCusto = BigDecimal(precoCusto)
            )
            p.estadoAtual = estado
            Mockito.`when`(produtoEstadoRepo.findByProdutoIdAndDataFimIsNull(p.id)).thenReturn(estado)
        } else {
            Mockito.`when`(produtoEstadoRepo.findByProdutoIdAndDataFimIsNull(p.id)).thenReturn(null)
        }
        return p
    }

    @Test
    fun `aplicarDelta positivo soma ao estoque e fecha o estado anterior abrindo um novo`() {
        val p = produto(estoque = 3)
        val estadoAnterior = p.estadoAtual!!
        Mockito.`when`(produtoRepo.findByIdForUpdate(p.id)).thenReturn(p)

        val novo = service.aplicarDelta(p.id, 5)

        assertNotNull(estadoAnterior.dataFim)
        assertEquals(8, novo.estoque)
        assertEquals(BigDecimal("10.00"), novo.preco)
        assertEquals(BigDecimal("5.00"), novo.precoCusto)
        assertSame(novo, p.estadoAtual)
        assertNotSame(estadoAnterior, novo)
    }

    @Test
    fun `aplicarDelta negativo que levaria o estoque abaixo de zero lanca EstadoInvalidoException`() {
        val p = produto(estoque = 3)
        val estadoAnterior = p.estadoAtual!!
        Mockito.`when`(produtoRepo.findByIdForUpdate(p.id)).thenReturn(p)

        assertThrows(EstadoInvalidoException::class.java) {
            service.aplicarDelta(p.id, -4)
        }

        // nada mudou: o estado nao foi fechado, nem o produto repontado
        assertNull(estadoAnterior.dataFim)
        assertSame(estadoAnterior, p.estadoAtual)
    }

    @Test
    fun `definir com preco e precoCusto nulos preserva os valores atuais`() {
        val p = produto(estoque = 3, preco = "20.00", precoCusto = "12.00")
        Mockito.`when`(produtoRepo.findByIdForUpdate(p.id)).thenReturn(p)

        val novo = service.definir(p.id, estoque = 9)

        assertEquals(9, novo.estoque)
        assertEquals(BigDecimal("20.00"), novo.preco)
        assertEquals(BigDecimal("12.00"), novo.precoCusto)
    }

    @Test
    fun `definir sem nenhuma mudanca nao abre estado novo`() {
        val p = produto(estoque = 3, preco = "20.00", precoCusto = "12.00")
        val estadoAnterior = p.estadoAtual!!
        Mockito.`when`(produtoRepo.findByIdForUpdate(p.id)).thenReturn(p)

        val resultado = service.definir(p.id, estoque = 3, preco = BigDecimal("20.00"), precoCusto = BigDecimal("12.00"))

        assertSame(estadoAnterior, resultado)
        assertNull(estadoAnterior.dataFim)
        Mockito.verify(produtoEstadoRepo, Mockito.never()).save(Mockito.any(ProdutoEstado::class.java))
    }

    @Test
    fun `produto sem estadoAtual lanca EstadoInvalidoException`() {
        val p = produto(estoque = null)
        Mockito.`when`(produtoRepo.findByIdForUpdate(p.id)).thenReturn(p)

        assertThrows(EstadoInvalidoException::class.java) {
            service.aplicarDelta(p.id, 1)
        }
    }

    @Test
    fun `produto inexistente lanca EntidadeNaoEncontradaException`() {
        val id = UUID.randomUUID()
        Mockito.`when`(produtoRepo.findByIdForUpdate(id)).thenReturn(null)

        assertThrows(EntidadeNaoEncontradaException::class.java) {
            service.aplicarDelta(id, 1)
        }
    }

    @Test
    fun `criarInicial cria o primeiro estado e reponta o produto`() {
        val p = produto(estoque = null)

        val estado = service.criarInicial(p, estoque = 10, preco = BigDecimal("15.00"), precoCusto = BigDecimal("7.00"))

        assertSame(estado, p.estadoAtual)
        assertEquals(10, estado.estoque)
        assertNull(estado.dataFim)
    }

    @Test
    fun `criarInicial com estoque negativo lanca EstadoInvalidoException`() {
        val p = produto(estoque = null)

        assertThrows(EstadoInvalidoException::class.java) {
            service.criarInicial(p, estoque = -1, preco = BigDecimal("15.00"), precoCusto = BigDecimal("7.00"))
        }

        assertNull(p.estadoAtual)
    }

    @Test
    fun `definir com produto sem estadoAtual cria o primeiro estado em vez de falhar`() {
        val p = produto(estoque = null)
        Mockito.`when`(produtoRepo.findByIdForUpdate(p.id)).thenReturn(p)

        val estado = service.definir(p.id, estoque = 10, preco = BigDecimal("15.00"), precoCusto = BigDecimal("7.00"))

        assertSame(estado, p.estadoAtual)
        assertEquals(10, estado.estoque)
        assertEquals(BigDecimal("15.00"), estado.preco)
        assertEquals(BigDecimal("7.00"), estado.precoCusto)
        assertNull(estado.dataFim)
    }

    @Test
    fun `definir com produto sem estadoAtual e sem preco informado usa zero como padrao`() {
        val p = produto(estoque = null)
        Mockito.`when`(produtoRepo.findByIdForUpdate(p.id)).thenReturn(p)

        val estado = service.definir(p.id, estoque = 10)

        assertEquals(BigDecimal.ZERO, estado.preco)
        assertEquals(BigDecimal.ZERO, estado.precoCusto)
    }
}
