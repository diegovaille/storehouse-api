package br.com.storehouse.service

import br.com.storehouse.constants.ErrorMessages
import br.com.storehouse.data.entities.ProdutoEstado
import br.com.storehouse.data.entities.Venda
import br.com.storehouse.data.entities.VendaItem
import br.com.storehouse.data.enums.TipoPagamento
import br.com.storehouse.data.model.ItemVendaResponse
import br.com.storehouse.data.model.ProdutoMaisVendidoResponse
import br.com.storehouse.data.model.ResumoVendasResponse
import br.com.storehouse.data.model.VendaDiaResponse
import br.com.storehouse.data.model.VendaRecenteResponse
import br.com.storehouse.data.model.VendaRequest
import br.com.storehouse.data.model.VendaResponse
import br.com.storehouse.data.repository.FilialRepository
import br.com.storehouse.data.repository.ProdutoRepository
import br.com.storehouse.data.repository.UsuarioRepository
import br.com.storehouse.data.repository.VendaRepository
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import br.com.storehouse.exceptions.EstadoInvalidoException
import br.com.storehouse.exceptions.RequisicaoInvalidaException
import br.com.storehouse.logging.LogCall
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Service
class VendaService(
    private val vendaRepo: VendaRepository,
    private val produtoRepo: ProdutoRepository,
    private val usuarioRepository: UsuarioRepository,
    private val filialRepository: FilialRepository
) {
    @LogCall
    @Transactional
    fun registrarVenda(
        filialId: UUID,
        request: VendaRequest,
        emailUsuario: String
    ): VendaResponse {

        val usuario = usuarioRepository.findByEmail(emailUsuario)
            ?: throw EntidadeNaoEncontradaException(ErrorMessages.USUARIO_NAO_ENCONTRADO)

        val filial = filialRepository.findByIdOrNull(filialId)
            ?: throw EntidadeNaoEncontradaException(ErrorMessages.FILIAL_NAO_ENCONTRADA)

        val venda = Venda(
            vendedor = usuario,
            valorTotal = BigDecimal.ZERO,
            filial = filial,
            voucher = request.voucher
        )

        val vendaItens = request.itens.map { item ->
            val produto = produtoRepo.findByCodigoBarrasAndFilialIdAndExcluidoFalse(item.codigoBarras, filialId)
                ?: throw EntidadeNaoEncontradaException("Produto ${item.codigoBarras} não encontrado na filial $filialId")

            val estadoAtual = produto.estadoAtual
                ?: throw EstadoInvalidoException("Produto ${item.codigoBarras} não possui estado atual definido")

            if (estadoAtual.estoque < item.quantidade) {
                throw EstadoInvalidoException("Estoque insuficiente para o produto ${item.codigoBarras}")
            }

            // Finaliza o estado atual
            estadoAtual.dataFim = LocalDateTime.now()

            val novoEstado = ProdutoEstado(
                produto = produto,
                preco = estadoAtual.preco,
                estoque = estadoAtual.estoque - item.quantidade,
                dataInicio = LocalDateTime.now(),
                precoCusto = estadoAtual.precoCusto // Preserva o custo do estado atual
            )

            produto.estadoAtual = novoEstado // Atualiza referência para o novo estado

            VendaItem(
                venda = venda,
                produto = produto,
                quantidade = item.quantidade,
                // Voucher será aplicado no TOTAL final, não no preço unitário.
                precoUnitario = estadoAtual.preco
            )
        }

        var total = calcularTotal(vendaItens)

        // aplica desconto se for voucher (50%)
        if (request.voucher) {
            total = total.divide(BigDecimal(2), 2, RoundingMode.HALF_UP)
        }

        venda.valorTotal = total
        venda.itens = vendaItens

        val pagamentos = request.pagamentos.map { pagamento ->

            val tipoPagamento = runCatching {
                TipoPagamento.valueOf(pagamento.tipo.uppercase())
            }.getOrElse {
                throw RequisicaoInvalidaException("Tipo de pagamento inválido: ${pagamento.tipo}")
            }

            br.com.storehouse.data.entities.VendaPagamento(
                venda = venda,
                tipo = tipoPagamento,
                valor = pagamento.valor
            )
        }

        venda.pagamentos = pagamentos // Adicione esse campo na entidade `Venda`

        return vendaRepo.save(venda).toResponse(false)
    }

    private fun calcularTotal(itens: List<VendaItem>): BigDecimal =
        itens.fold(BigDecimal.ZERO) { acc, item ->
            acc + item.precoUnitario.multiply(BigDecimal.valueOf(item.quantidade.toLong()))
        }

    fun listarVendas(): List<VendaResponse> = vendaRepo.findAll().map { it.toResponse(false) }

    @LogCall
    fun listarVendasPorPeriodo(filialId: UUID, inicio: String?, fim: String?, apenasAtiva: Boolean, relatorio: Boolean): List<VendaResponse> {
        val dataInicio = inicio?.let { LocalDateTime.parse("${it}T00:00:00") }
            ?: LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
        val dataFim = fim?.let { LocalDateTime.parse("${it}T23:59:59") }
            ?: LocalDateTime.now().withHour(23).withMinute(59).withSecond(59)


        return vendaRepo.findByFilialIdAndDataBetweenOrderByDataDesc(filialId, dataInicio, dataFim)
            .filter { venda -> !apenasAtiva || !venda.cancelada }
            .map { it.toResponse(relatorio) }
    }

    private fun inicioDoDia(inicio: String?): LocalDateTime =
        inicio?.let { LocalDateTime.parse("${it}T00:00:00") }
            ?: LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)

    private fun fimDoDia(fim: String?): LocalDateTime =
        fim?.let { LocalDateTime.parse("${it}T23:59:59") }
            ?: LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(0)

    private fun vendasAtivas(filialId: UUID, inicio: LocalDateTime, fim: LocalDateTime): List<Venda> =
        vendaRepo.findByFilialIdAndDataBetweenOrderByDataDesc(filialId, inicio, fim)
            .filter { !it.cancelada }

    @LogCall
    fun resumoVendas(filialId: UUID, inicio: String?, fim: String?): ResumoVendasResponse {
        val todas = vendaRepo.findByFilialIdAndDataBetweenOrderByDataDesc(filialId, inicioDoDia(inicio), fimDoDia(fim))
        val ativas = todas.filter { !it.cancelada }
        val quantidade = ativas.size
        val total = ativas.fold(BigDecimal.ZERO) { acc, v -> acc + v.valorTotal }
        val ticket = if (quantidade == 0) BigDecimal.ZERO
            else total.divide(BigDecimal(quantidade), 2, RoundingMode.HALF_UP)
        return ResumoVendasResponse(
            quantidade = quantidade,
            totalArrecadado = total,
            ticketMedio = ticket,
            vouchersUsados = ativas.count { it.voucher },
            cancelados = todas.count { it.cancelada }
        )
    }

    @LogCall
    fun vendasRecentes(filialId: UUID, limite: Int): List<VendaRecenteResponse> =
        vendasAtivas(filialId, inicioDoDia(null), fimDoDia(null))
            .take(limite)
            .map { v ->
                VendaRecenteResponse(
                    id = v.id,
                    data = v.data.toString(),
                    metodos = v.pagamentos.map { it.tipo.name }.distinct(),
                    valorTotal = v.valorTotal
                )
            }

    @LogCall
    fun maisVendidos(
        filialId: UUID, inicio: String?, fim: String?, limite: Int, categoria: String?
    ): List<ProdutoMaisVendidoResponse> {
        data class Agg(val nome: String, val categoria: String, var qtd: Int, var total: BigDecimal)
        val acc = LinkedHashMap<String, Agg>()
        vendasAtivas(filialId, inicioDoDia(inicio), fimDoDia(fim)).forEach { v ->
            v.itens.forEach { item ->
                val cat = item.produto.tipo.nome
                if (categoria == null || cat.equals(categoria, ignoreCase = true)) {
                    val agg = acc.getOrPut(item.produto.nome) { Agg(item.produto.nome, cat, 0, BigDecimal.ZERO) }
                    agg.qtd += item.quantidade
                    agg.total += item.precoUnitario.multiply(BigDecimal(item.quantidade))
                }
            }
        }
        return acc.values.sortedByDescending { it.qtd }.take(limite)
            .map { ProdutoMaisVendidoResponse(it.nome, it.categoria, it.qtd, it.total) }
    }

    @LogCall
    fun serieVendas(filialId: UUID, dias: Int): List<VendaDiaResponse> {
        val hoje = LocalDate.now()
        val inicioDate = hoje.minusDays((dias - 1).toLong())
        val vendas = vendasAtivas(filialId, inicioDate.atStartOfDay(), hoje.atTime(23, 59, 59))
        val porDia = vendas.groupBy { it.data.toLocalDate() }
        return (0 until dias).map { offset ->
            val dia = inicioDate.plusDays(offset.toLong())
            val doDia = porDia[dia] ?: emptyList()
            VendaDiaResponse(
                data = dia,
                quantidade = doDia.size,
                total = doDia.fold(BigDecimal.ZERO) { acc, v -> acc + v.valorTotal }
            )
        }
    }

    @LogCall
    @Transactional
    fun cancelarVenda(filialId: UUID, id: String) {
        val venda = vendaRepo.findByIdOrNull(UUID.fromString(id))
            ?: throw EntidadeNaoEncontradaException("Venda com ID $id não encontrada")

        if (venda.filial.id != filialId) {
            throw EntidadeNaoEncontradaException("Venda com ID $id não pertence à filial $filialId")
        }

        // Verifica se a venda já foi cancelada
        if (venda.cancelada) {
            throw RequisicaoInvalidaException("Venda com ID $id já foi cancelada")
        }

        // Marca a venda como cancelada
        venda.cancelada = true

        // Atualiza o estoque dos produtos vendidos
        venda.itens.forEach { item ->
            val produto = item.produto
            val estadoAtual = produto.estadoAtual ?: return@forEach

            // Restaura o estoque do produto
            estadoAtual.estoque += item.quantidade
            produto.estadoAtual = estadoAtual
        }

        vendaRepo.save(venda)
    }
}

fun Venda.toResponse(relatorio: Boolean): VendaResponse = VendaResponse(
    id = this.id,
    valorTotal = this.valorTotal,
    data = this.data.toString(),
    vendedorNome = this.vendedor.username ?: "Desconhecido",
    vendedorEmail = this.vendedor.email,
    cancelada = this.cancelada,
    itens = this.itens.map {
        ItemVendaResponse(
            produtoNome = it.produto.nome,
            categoria = it.produto.tipo.nome,
            quantidade = it.quantidade,
            precoUnitario = it.precoUnitario,
            estoque = if (relatorio) it.produto.estadoAtual!!.estoque else null,
            precoCusto = if (relatorio) it.produto.estadoAtual!!.precoCusto else null,
        )
    }
)
