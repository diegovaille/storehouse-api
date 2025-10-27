package br.com.storehouse.service

import br.com.storehouse.constants.ErrorMessages
import br.com.storehouse.data.entities.ProdutoEstado
import br.com.storehouse.data.entities.Venda
import br.com.storehouse.data.entities.VendaItem
import br.com.storehouse.data.enums.TipoPagamento
import br.com.storehouse.data.model.ItemVendaResponse
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
                precoUnitario = if (venda.voucher) estadoAtual.preco.divide(BigDecimal(2)) else estadoAtual.preco
            )
        }

        var total = calcularTotal(vendaItens)

        // aplica desconto se for voucher
        if (request.voucher) {
            total = total.divide(BigDecimal(2))
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


        val listaVendas = vendaRepo.findByFilialIdAndDataBetweenOrderByDataDesc(filialId, dataInicio, dataFim)
        return vendaRepo.findByFilialIdAndDataBetweenOrderByDataDesc(filialId, dataInicio, dataFim)
            .filter { venda -> !apenasAtiva || !venda.cancelada }
            .map { it.toResponse(relatorio) }
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
    valorTotal = this.valorTotal ?: BigDecimal.ZERO,
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
