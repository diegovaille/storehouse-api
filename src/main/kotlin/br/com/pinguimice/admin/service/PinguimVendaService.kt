package br.com.pinguimice.admin.service

import br.com.pinguimice.admin.entity.PinguimVenda
import br.com.pinguimice.admin.entity.PinguimVendaItem
import br.com.pinguimice.admin.model.PinguimVendaItemResponse
import br.com.pinguimice.admin.model.PinguimVendaRequest
import br.com.pinguimice.admin.model.PinguimVendaResponse
import br.com.pinguimice.admin.repository.EstoqueGelinhoRepository
import br.com.pinguimice.admin.repository.PinguimVendaRepository
import br.com.pinguimice.admin.repository.RegiaoVendaRepository
import br.com.pinguimice.admin.repository.SaborRepository
import br.com.storehouse.data.repository.UsuarioRepository
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import br.com.storehouse.exceptions.EstadoInvalidoException
import br.com.storehouse.logging.LogCall
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class PinguimVendaService(
    private val vendaRepository: PinguimVendaRepository,
    private val regiaoRepository: RegiaoVendaRepository,
    private val saborRepository: SaborRepository,
    private val estoqueRepository: EstoqueGelinhoRepository,
    private val usuarioRepository: UsuarioRepository
) {

    @LogCall
    @Transactional
    fun registrarVenda(request: PinguimVendaRequest, emailUsuario: String): PinguimVendaResponse {
        val usuario = usuarioRepository.findByEmail(emailUsuario)
            ?: throw EntidadeNaoEncontradaException("Usuário não encontrado")

        val regiao = regiaoRepository.findByIdOrNull(request.regiaoId)
            ?: throw EntidadeNaoEncontradaException("Região não encontrada")

        val venda = PinguimVenda(
            total = request.total,
            totalPago = request.totalPago,
            cliente = request.cliente,
            regiao = regiao,
            usuarioId = usuario.id
        )

        val itens = request.itens.map { itemRequest ->
            val sabor = saborRepository.findByIdOrNull(itemRequest.saborId)
                ?: throw EntidadeNaoEncontradaException("Sabor não encontrado")

            // Atualizar estoque
            val estoque = estoqueRepository.findBySaborId(sabor.id)
                ?: throw EntidadeNaoEncontradaException("Estoque não encontrado para o sabor ${sabor.nome}")

            if (estoque.quantidade < itemRequest.quantidade) {
                throw EstadoInvalidoException("Estoque insuficiente para o sabor ${sabor.nome}")
            }

            estoque.quantidade -= itemRequest.quantidade
            estoque.ultimaAtualizacao = LocalDateTime.now()
            estoqueRepository.save(estoque)

            PinguimVendaItem(
                venda = venda,
                sabor = sabor,
                quantidade = itemRequest.quantidade
            )
        }

        venda.itens = itens
        val vendaSalva = vendaRepository.save(venda)

        return vendaSalva.toResponse(usuario.username ?: "Desconhecido")
    }

    @LogCall
    fun listarVendas(inicio: LocalDateTime?, fim: LocalDateTime?): List<PinguimVendaResponse> {
        val dataInicio = inicio ?: LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
        val dataFim = fim ?: LocalDateTime.now().withHour(23).withMinute(59).withSecond(59)

        val vendas = vendaRepository.findByDataVendaBetweenOrderByDataVendaDesc(dataInicio, dataFim)

        // Para simplificar, vamos buscar o nome do usuário de cada venda.
        // Em um cenário de alta performance, poderíamos fazer um fetch join ou cache.
        return vendas.map { venda ->
            val usuario = usuarioRepository.findByIdOrNull(venda.usuarioId)
            venda.toResponse(usuario?.username ?: "Desconhecido")
        }
    }

    @LogCall
    @Transactional
    fun cancelarVenda(id: UUID) {
        val venda = vendaRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Venda não encontrada")

        // Restaurar estoque
        venda.itens.forEach { item ->
            val estoque = estoqueRepository.findBySaborId(item.sabor.id)
            if (estoque != null) {
                estoque.quantidade += item.quantidade
                estoque.ultimaAtualizacao = LocalDateTime.now()
                estoqueRepository.save(estoque)
            }
        }

        vendaRepository.delete(venda)
    }

    private fun PinguimVenda.toResponse(nomeVendedor: String): PinguimVendaResponse {
        return PinguimVendaResponse(
            id = this.id,
            total = this.total,
            totalPago = this.totalPago,
            dataVenda = this.dataVenda.toString(),
            cliente = this.cliente,
            regiao = this.regiao.nome,
            vendedor = nomeVendedor,
            itens = this.itens.map {
                PinguimVendaItemResponse(
                    sabor = it.sabor.nome,
                    quantidade = it.quantidade
                )
            }
        )
    }
}
