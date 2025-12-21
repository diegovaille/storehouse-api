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
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(PinguimVendaService::class.java)

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
            usuarioId = usuario.id,
            abaterEstoque = request.abaterEstoque
        )

        val itens = request.itens.map { itemRequest ->
            val sabor = saborRepository.findByIdOrNull(itemRequest.saborId)
                ?: throw EntidadeNaoEncontradaException("Sabor não encontrado")

            // Atualizar estoque apenas se abaterEstoque for true
            if (request.abaterEstoque) {
                val estoque = estoqueRepository.findBySaborId(sabor.id)
                    ?: throw EntidadeNaoEncontradaException("Estoque não encontrado para o sabor ${sabor.nome}")

                if (estoque.quantidade < itemRequest.quantidade) {
                    throw EstadoInvalidoException("Estoque insuficiente para o sabor ${sabor.nome}")
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

        venda.itens = itens
        val vendaSalva = vendaRepository.save(venda)

        return vendaSalva.toResponse(usuario.username ?: "Desconhecido")
    }

    @LogCall
    fun listarVendas(inicio: LocalDateTime?, fim: LocalDateTime?): List<PinguimVendaResponse> {
        val dataInicio = inicio ?: LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
        val dataFim = fim ?: LocalDateTime.now().withHour(23).withMinute(59).withSecond(59)

        val vendas = vendaRepository.findByDataVendaBetweenOrderByDataVendaDesc(dataInicio, dataFim)

        logger.info("Encontradas ${vendas.size} vendas entre $dataInicio e $dataFim")
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

        // Restaurar estoque apenas se foi abatido na criação
        if (venda.abaterEstoque) {
            venda.itens.forEach { item ->
                val estoque = estoqueRepository.findBySaborId(item.sabor.id)
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
    fun editarVenda(id: UUID, request: PinguimVendaRequest, emailUsuario: String): PinguimVendaResponse {
        val usuario = usuarioRepository.findByEmail(emailUsuario)
            ?: throw EntidadeNaoEncontradaException("Usuário não encontrado")

        val vendaExistente = vendaRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Venda não encontrada")

        val regiao = regiaoRepository.findByIdOrNull(request.regiaoId)
            ?: throw EntidadeNaoEncontradaException("Região não encontrada")

        // Análise de mudança de estoque
        val abaterEstoqueAntes = vendaExistente.abaterEstoque
        val abaterEstoqueAgora = request.abaterEstoque

        // Se estava abatendo e agora não abate mais, restaurar o estoque anterior
        if (abaterEstoqueAntes && !abaterEstoqueAgora) {
            vendaExistente.itens.forEach { item ->
                val estoque = estoqueRepository.findBySaborId(item.sabor.id)
                if (estoque != null) {
                    estoque.quantidade += item.quantidade
                    estoque.ultimaAtualizacao = LocalDateTime.now()
                    estoqueRepository.save(estoque)
                }
            }
        }

        // Se não estava abatendo e agora vai abater, deduzir o estoque
        if (!abaterEstoqueAntes && abaterEstoqueAgora) {
            request.itens.forEach { itemRequest ->
                val sabor = saborRepository.findByIdOrNull(itemRequest.saborId)
                    ?: throw EntidadeNaoEncontradaException("Sabor não encontrado")

                val estoque = estoqueRepository.findBySaborId(sabor.id)
                    ?: throw EntidadeNaoEncontradaException("Estoque não encontrado para o sabor ${sabor.nome}")

                if (estoque.quantidade < itemRequest.quantidade) {
                    throw EstadoInvalidoException("Estoque insuficiente para o sabor ${sabor.nome}")
                }

                estoque.quantidade -= itemRequest.quantidade
                estoque.ultimaAtualizacao = LocalDateTime.now()
                estoqueRepository.save(estoque)
            }
        }

        // Se continuou abatendo, ajustar diferenças de quantidade
        if (abaterEstoqueAntes && abaterEstoqueAgora) {
            // Criar mapa de quantidades antigas por sabor
            val quantidadesAntigas = vendaExistente.itens.associate { it.sabor.id to it.quantidade }
            val quantidadesNovas = request.itens.associate { it.saborId to it.quantidade }

            // Restaurar quantidades dos sabores removidos ou com quantidade reduzida
            quantidadesAntigas.forEach { (saborId, quantidadeAntiga) ->
                val quantidadeNova = quantidadesNovas[saborId] ?: 0
                val diferenca = quantidadeAntiga - quantidadeNova

                if (diferenca > 0) {
                    val estoque = estoqueRepository.findBySaborId(saborId)
                    if (estoque != null) {
                        estoque.quantidade += diferenca
                        estoque.ultimaAtualizacao = LocalDateTime.now()
                        estoqueRepository.save(estoque)
                    }
                }
            }

            // Deduzir quantidades dos sabores novos ou com quantidade aumentada
            quantidadesNovas.forEach { (saborId, quantidadeNova) ->
                val quantidadeAntiga = quantidadesAntigas[saborId] ?: 0
                val diferenca = quantidadeNova - quantidadeAntiga

                if (diferenca > 0) {
                    val sabor = saborRepository.findByIdOrNull(saborId)
                        ?: throw EntidadeNaoEncontradaException("Sabor não encontrado")

                    val estoque = estoqueRepository.findBySaborId(saborId)
                        ?: throw EntidadeNaoEncontradaException("Estoque não encontrado para o sabor ${sabor.nome}")

                    if (estoque.quantidade < diferenca) {
                        throw EstadoInvalidoException("Estoque insuficiente para o sabor ${sabor.nome}")
                    }

                    estoque.quantidade -= diferenca
                    estoque.ultimaAtualizacao = LocalDateTime.now()
                    estoqueRepository.save(estoque)
                }
            }
        }

        // Atualizar dados da venda
        vendaExistente.total = request.total
        vendaExistente.totalPago = request.totalPago
        vendaExistente.cliente = request.cliente
        vendaExistente.regiao = regiao
        vendaExistente.abaterEstoque = request.abaterEstoque

        // Atualizar itens
        vendaExistente.itens = request.itens.map { itemRequest ->
            val sabor = saborRepository.findByIdOrNull(itemRequest.saborId)
                ?: throw EntidadeNaoEncontradaException("Sabor não encontrado")

            PinguimVendaItem(
                venda = vendaExistente,
                sabor = sabor,
                quantidade = itemRequest.quantidade
            )
        }

        val vendaSalva = vendaRepository.save(vendaExistente)
        return vendaSalva.toResponse(usuario.username ?: "Desconhecido")
    }

    @LogCall
    @Transactional
    fun marcarComoPaga(id: UUID, emailUsuario: String): PinguimVendaResponse {
        val usuario = usuarioRepository.findByEmail(emailUsuario)
            ?: throw EntidadeNaoEncontradaException("Usuário não encontrado")

        val venda = vendaRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Venda não encontrada")

        // Marca como paga igualando totalPago ao total
        venda.totalPago = venda.total

        val vendaSalva = vendaRepository.save(venda)
        return vendaSalva.toResponse(usuario.username ?: "Desconhecido")
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
