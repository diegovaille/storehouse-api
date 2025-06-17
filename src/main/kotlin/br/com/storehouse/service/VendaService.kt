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
            filial = filial
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
                precoUnitario = estadoAtual.preco
            )
        }

        venda.valorTotal = calcularTotal(vendaItens)
        venda.itens = vendaItens

        val pagamentos = request.pagamentos.map { pagamento ->

            val tipoPagamento = runCatching {
                TipoPagamento.valueOf(pagamento.tipo.uppercase())
            }.getOrElse {
                throw br.com.storehouse.exceptions.RequisicaoInvalidaException("Tipo de pagamento inválido: ${pagamento.tipo}")
            }

            br.com.storehouse.data.entities.VendaPagamento(
                venda = venda,
                tipo = tipoPagamento,
                valor = pagamento.valor
            )
        }

        venda.pagamentos = pagamentos // Adicione esse campo na entidade `Venda`

        return vendaRepo.save(venda).toResponse()
    }

    private fun calcularTotal(itens: List<VendaItem>): BigDecimal =
        itens.fold(BigDecimal.ZERO) { acc, item ->
            acc + item.precoUnitario.multiply(BigDecimal.valueOf(item.quantidade.toLong()))
        }

    fun listarVendas(): List<VendaResponse> = vendaRepo.findAll().map { it.toResponse() }

    fun listarVendasPorPeriodo(filialId: UUID, inicio: String?, fim: String?): List<VendaResponse> {
        val dataInicio = inicio?.let { LocalDateTime.parse("${it}T00:00:00") }
            ?: LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
        val dataFim = fim?.let { LocalDateTime.parse("${it}T23:59:59") }
            ?: LocalDateTime.now().withHour(23).withMinute(59).withSecond(59)

        return vendaRepo.findByFilialIdAndDataBetweenOrderByDataDesc(filialId, dataInicio, dataFim)
            .map { it.toResponse() }
    }
}

fun Venda.toResponse(): VendaResponse = VendaResponse(
    id = this.id,
    valorTotal = this.valorTotal ?: BigDecimal.ZERO,
    data = this.data.toString(),
    vendedorNome = this.vendedor.username ?: "Desconhecido",
    vendedorEmail = this.vendedor.email,
    itens = this.itens.map {
        ItemVendaResponse(
            produtoNome = it.produto.nome,
            quantidade = it.quantidade,
            precoUnitario = it.precoUnitario
        )
    }
)
