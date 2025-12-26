package br.com.pinguimice.admin.service

import br.com.pinguimice.admin.entity.PinguimVenda
import br.com.pinguimice.admin.entity.PinguimVendaItem
import br.com.pinguimice.admin.model.PinguimVendaItemResponse
import br.com.pinguimice.admin.model.PinguimVendaRequest
import br.com.pinguimice.admin.model.PinguimVendaResponse
import br.com.pinguimice.admin.repository.ClienteRepository
import br.com.pinguimice.admin.repository.EstoqueGelinhoRepository
import br.com.pinguimice.admin.repository.PinguimVendaRepository
import br.com.pinguimice.admin.repository.SaborRepository
import br.com.storehouse.data.repository.UsuarioRepository
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import br.com.storehouse.exceptions.EstadoInvalidoException
import br.com.storehouse.logging.LogCall
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class PinguimVendaService(
    private val vendaRepository: PinguimVendaRepository,
    private val saborRepository: SaborRepository,
    private val estoqueRepository: EstoqueGelinhoRepository,
    private val usuarioRepository: UsuarioRepository,
    private val clienteRepository: ClienteRepository,
    private val filialProvider: FilialProvider
) {
    private val logger = LoggerFactory.getLogger(PinguimVendaService::class.java)

    @LogCall
    @Transactional
    fun registrarVenda(request: PinguimVendaRequest, filialId: UUID, emailUsuario: String): PinguimVendaResponse {
        val usuario = usuarioRepository.findByEmail(emailUsuario)
            ?: throw EntidadeNaoEncontradaException("Usuário não encontrado")

        val clienteRef = request.clienteId?.let {
            val cliente = clienteRepository.findByIdOrNull(it)
                ?: throw EntidadeNaoEncontradaException("Cliente não encontrado")
            if (cliente.filial.id != filialId) throw EntidadeNaoEncontradaException("Cliente não pertence à filial informada")
            cliente
        }

        val filial = filialProvider.get(filialId)

        val venda = PinguimVenda(
            total = request.total,
            totalPago = request.totalPago,
            cliente = clienteRef,
            usuarioId = usuario.id,
            abaterEstoque = request.abaterEstoque,
            filial = filial
        )

        val itens = request.itens.map { itemRequest ->
            val sabor = saborRepository.findByIdOrNull(itemRequest.saborId)
                ?: throw EntidadeNaoEncontradaException("Sabor não encontrado")
            if (sabor.filial.id != filialId) throw EntidadeNaoEncontradaException("Sabor não pertence à filial informada")

            if (request.abaterEstoque) {
                val estoque = estoqueRepository.findBySaborIdAndFilialId(sabor.id, filialId)
                    ?: throw EntidadeNaoEncontradaException("Estoque não encontrado para o sabor ${'$'}{sabor.nome}")

                if (estoque.quantidade < itemRequest.quantidade) {
                    throw EstadoInvalidoException("Estoque insuficiente para o sabor ${'$'}{sabor.nome}")
                }

                estoque.quantidade -= itemRequest.quantidade
                estoque.ultimaAtualizacao = LocalDateTime.now()
                estoqueRepository.save(estoque)
            }

            PinguimVendaItem(
                venda = venda,
                sabor = sabor,
                quantidade = itemRequest.quantidade
            )
        }

        venda.itens.clear()
        venda.itens.addAll(itens)
        val vendaSalva = vendaRepository.save(venda)

        return vendaSalva.toResponse(usuario.username ?: "Desconhecido")
    }

    @LogCall
    fun listarVendas(inicio: LocalDateTime?, fim: LocalDateTime?, filialId: UUID): List<PinguimVendaResponse> {
        val dataInicio = inicio ?: LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
        val dataFim = fim ?: LocalDateTime.now().withHour(23).withMinute(59).withSecond(59)

        val vendas = vendaRepository.findByFilialIdAndDataVendaBetweenOrderByDataVendaDesc(filialId, dataInicio, dataFim)

        logger.info("Encontradas ${'$'}{vendas.size} vendas entre ${'$'}dataInicio e ${'$'}dataFim")
        return vendas.map { venda ->
            val usuario = usuarioRepository.findByIdOrNull(venda.usuarioId)
            venda.toResponse(usuario?.username ?: "Desconhecido")
        }
    }

    @LogCall
    @Transactional
    fun cancelarVenda(id: UUID, filialId: UUID) {
        val venda = vendaRepository.findByIdAndFilialId(id, filialId)
            ?: throw EntidadeNaoEncontradaException("Venda não encontrada")

        // Restaurar estoque apenas se foi abatido na criação
        if (venda.abaterEstoque) {
            venda.itens.forEach { item ->
                val estoque = estoqueRepository.findBySaborIdAndFilialId(item.sabor.id, filialId)
                if (estoque != null) {
                    estoque.quantidade += item.quantidade
                    estoque.ultimaAtualizacao = LocalDateTime.now()
                    estoqueRepository.save(estoque)
                }
            }
        }

        vendaRepository.delete(venda)
    }

    @LogCall
    @Transactional
    fun editarVenda(id: UUID, request: PinguimVendaRequest, filialId: UUID, emailUsuario: String): PinguimVendaResponse {
        val usuario = usuarioRepository.findByEmail(emailUsuario)
            ?: throw EntidadeNaoEncontradaException("Usuário não encontrado")

        val vendaExistente = vendaRepository.findByIdAndFilialId(id, filialId)
            ?: throw EntidadeNaoEncontradaException("Venda não encontrada")

        val clienteRef = request.clienteId?.let {
            val cliente = clienteRepository.findByIdOrNull(it)
                ?: throw EntidadeNaoEncontradaException("Cliente não encontrado")
            if (cliente.filial.id != filialId) throw EntidadeNaoEncontradaException("Cliente não pertence à filial informada")
            cliente
        }

        val abaterEstoqueAntes = vendaExistente.abaterEstoque
        val abaterEstoqueAgora = request.abaterEstoque

        if (abaterEstoqueAntes && !abaterEstoqueAgora) {
            vendaExistente.itens.forEach { item ->
                val estoque = estoqueRepository.findBySaborIdAndFilialId(item.sabor.id, filialId)
                if (estoque != null) {
                    estoque.quantidade += item.quantidade
                    estoque.ultimaAtualizacao = LocalDateTime.now()
                    estoqueRepository.save(estoque)
                }
            }
        }

        if (!abaterEstoqueAntes && abaterEstoqueAgora) {
            request.itens.forEach { itemRequest ->
                val sabor = saborRepository.findByIdOrNull(itemRequest.saborId)
                    ?: throw EntidadeNaoEncontradaException("Sabor não encontrado")
                if (sabor.filial.id != filialId) throw EntidadeNaoEncontradaException("Sabor não pertence à filial informada")

                val estoque = estoqueRepository.findBySaborIdAndFilialId(sabor.id, filialId)
                    ?: throw EntidadeNaoEncontradaException("Estoque não encontrado para o sabor ${'$'}{sabor.nome}")

                if (estoque.quantidade < itemRequest.quantidade) {
                    throw EstadoInvalidoException("Estoque insuficiente para o sabor ${'$'}{sabor.nome}")
                }

                estoque.quantidade -= itemRequest.quantidade
                estoque.ultimaAtualizacao = LocalDateTime.now()
                estoqueRepository.save(estoque)
            }
        }

        if (abaterEstoqueAntes && abaterEstoqueAgora) {
            val quantidadesAntigas = vendaExistente.itens.associate { it.sabor.id to it.quantidade }
            val quantidadesNovas = request.itens.associate { it.saborId to it.quantidade }

            quantidadesAntigas.forEach { (saborId, quantidadeAntiga) ->
                val quantidadeNova = quantidadesNovas[saborId] ?: 0
                val diferenca = quantidadeAntiga - quantidadeNova

                if (diferenca > 0) {
                    val estoque = estoqueRepository.findBySaborIdAndFilialId(saborId, filialId)
                    if (estoque != null) {
                        estoque.quantidade += diferenca
                        estoque.ultimaAtualizacao = LocalDateTime.now()
                        estoqueRepository.save(estoque)
                    }
                }
            }

            quantidadesNovas.forEach { (saborId, quantidadeNova) ->
                val quantidadeAntiga = quantidadesAntigas[saborId] ?: 0
                val diferenca = quantidadeNova - quantidadeAntiga

                if (diferenca > 0) {
                    val sabor = saborRepository.findByIdOrNull(saborId)
                        ?: throw EntidadeNaoEncontradaException("Sabor não encontrado")
                    if (sabor.filial.id != filialId) throw EntidadeNaoEncontradaException("Sabor não pertence à filial informada")

                    val estoque = estoqueRepository.findBySaborIdAndFilialId(saborId, filialId)
                        ?: throw EntidadeNaoEncontradaException("Estoque não encontrado para o sabor ${'$'}{sabor.nome}")

                    if (estoque.quantidade < diferenca) {
                        throw EstadoInvalidoException("Estoque insuficiente para o sabor ${'$'}{sabor.nome}")
                    }

                    estoque.quantidade -= diferenca
                    estoque.ultimaAtualizacao = LocalDateTime.now()
                    estoqueRepository.save(estoque)
                }
            }
        }

        vendaExistente.total = request.total
        vendaExistente.totalPago = request.totalPago
        vendaExistente.cliente = clienteRef
        vendaExistente.abaterEstoque = request.abaterEstoque

        // Atualizar itens (IMPORTANTE: não reatribuir a coleção por causa do orphanRemoval)
        val novosItens = request.itens.map { itemRequest ->
            val sabor = saborRepository.findByIdOrNull(itemRequest.saborId)
                ?: throw EntidadeNaoEncontradaException("Sabor não encontrado")
            if (sabor.filial.id != filialId) throw EntidadeNaoEncontradaException("Sabor não pertence à filial informada")

            PinguimVendaItem(
                venda = vendaExistente,
                sabor = sabor,
                quantidade = itemRequest.quantidade
            )
        }

        vendaExistente.itens.clear()
        vendaExistente.itens.addAll(novosItens)

        val vendaSalva = vendaRepository.save(vendaExistente)
        return vendaSalva.toResponse(usuario.username ?: "Desconhecido")
    }

    @LogCall
    @Transactional
    fun marcarComoPaga(id: UUID, filialId: UUID, emailUsuario: String): PinguimVendaResponse {
        val usuario = usuarioRepository.findByEmail(emailUsuario)
            ?: throw EntidadeNaoEncontradaException("Usuário não encontrado")

        val venda = vendaRepository.findByIdAndFilialId(id, filialId)
            ?: throw EntidadeNaoEncontradaException("Venda não encontrada")

        venda.totalPago = venda.total
        val vendaSalva = vendaRepository.save(venda)
        return vendaSalva.toResponse(usuario.username ?: "Desconhecido")
    }

    private fun PinguimVenda.toResponse(nomeVendedor: String): PinguimVendaResponse {
        val regiao = this.cliente?.regiao
        return PinguimVendaResponse(
            id = this.id,
            total = this.total,
            totalPago = this.totalPago,
            dataVenda = this.dataVenda.toString(),
            clienteId = this.cliente?.id,
            clienteNome = this.cliente?.nome,
            regiaoId = regiao?.id,
            regiaoNome = regiao?.nome,
            vendedor = nomeVendedor,
            abaterEstoque = this.abaterEstoque,
            itens = this.itens.map {
                PinguimVendaItemResponse(
                    sabor = it.sabor.nome,
                    quantidade = it.quantidade
                )
            }
        )
    }
}
