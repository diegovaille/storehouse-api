package br.com.storehouse.service

import br.com.storehouse.constants.ErrorMessages
import br.com.storehouse.data.entities.Filial
import br.com.storehouse.data.entities.Produto
import br.com.storehouse.data.entities.ProdutoEstado
import br.com.storehouse.data.entities.TipoProduto
import br.com.storehouse.data.model.ProdutoDto
import br.com.storehouse.data.repository.*
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import br.com.storehouse.exceptions.RequisicaoInvalidaException
import br.com.storehouse.logging.LogCall
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class ProdutoService(
    private val produtoRepository: ProdutoRepository,
    private val tipoProdutoRepository: TipoProdutoRepository,
    private val produtoEstadoRepository: ProdutoEstadoRepository,
    private val produtoDescricaoRepository: ProdutoDescricaoRepository,
    private val filialRepository: FilialRepository,
    private val storageService: StorageService
) {
    private val objectMapper = jacksonObjectMapper()
    private val logger = LoggerFactory.getLogger(ProdutoService::class.java)

    @Transactional
    fun cadastrarOuAtualizar(filialId: UUID, dto: ProdutoDto, imagem: ByteArray?): Produto {
        val tipoProduto = tipoProdutoRepository.findByIdOrNull(dto.tipoId)
            ?: throw EntidadeNaoEncontradaException(ErrorMessages.TIPO_PRODUTO_NAO_ENCONTRADO)

        val filial = filialRepository.findByIdOrNull(filialId)
            ?: throw EntidadeNaoEncontradaException(ErrorMessages.FILIAL_NAO_ENCONTRADA)

        validarProduto(dto, tipoProduto)

        val imagemUrlFinal = when {
            imagem != null -> storageService.uploadImagemProduto(filialId, dto.codigoBarras, imagem)
            !dto.imagemUrl.isNullOrBlank() -> dto.imagemUrl
            else -> null
        }

        val existente: Produto? = if (!dto.id.isNullOrBlank()) {
            val uuid = UUID.fromString(dto.id)
            produtoRepository.findById(uuid).orElseThrow {
                EntidadeNaoEncontradaException("Produto com id ${dto.id} não encontrado.")
            }
        } else {
            produtoRepository.findByCodigoBarrasAndFilialIdAndExcluidoFalse(
                dto.codigoBarras,
                filialId
            )
        }

        return if (existente == null) {
            criarNovoProduto(dto, tipoProduto, filial, imagemUrlFinal)
        } else {
            atualizarProdutoExistente(existente, dto, tipoProduto, imagemUrlFinal)
        }
    }

    private fun criarNovoProduto(
        dto: ProdutoDto,
        tipo: TipoProduto,
        filial: Filial,
        imagemUrl: String?
    ): Produto {
        val produto = Produto(
            codigoBarras = dto.codigoBarras,
            filial = filial,
            tipo = tipo,
            nome = dto.nome,
            imagemUrl = imagemUrl,
            excluido = false
        )

        val salvo = produtoRepository.save(produto)

        val estado = ProdutoEstado(
            produto = salvo,
            estoque = dto.estoque,
            preco = dto.preco,
            precoCusto = dto.precoCusto,
        )

        produtoEstadoRepository.save(estado)

        salvo.estadoAtual = estado
        val atualizado = produtoRepository.save(salvo)

        dto.descricaoCampos?.let {
            val descricao = br.com.storehouse.data.entities.ProdutoDescricao(produto = atualizado, descricaoCampos = it)
            produtoDescricaoRepository.save(descricao)
        }

        logger.info("Novo produto cadastrado: ${atualizado.codigoBarras} - Filial: ${filial.id}")
        return atualizado
    }

    private fun atualizarProdutoExistente(
        produto: Produto,
        dto: ProdutoDto,
        tipoProduto: TipoProduto,
        imagemUrl: String?
    ): Produto {
        produto.nome = dto.nome
        produto.tipo = tipoProduto
        produto.codigoBarras = dto.codigoBarras

        if (imagemUrl != null) produto.imagemUrl = imagemUrl
        produto.excluido = false

        if (precisaNovoEstado(produto.estadoAtual, dto)) {
            produto.estadoAtual?.let {
                it.dataFim = LocalDateTime.now()
                produtoEstadoRepository.save(it)
            }

            val novoEstado = ProdutoEstado(
                produto = produto,
                estoque = dto.estoque,
                preco = dto.preco,
                precoCusto = dto.precoCusto
            )
            produtoEstadoRepository.save(novoEstado)
            produto.estadoAtual = novoEstado
        }

        val atualizado = produtoRepository.save(produto)

        val descricao = produtoDescricaoRepository.findByProdutoId(atualizado.id)
            ?.apply {
                descricaoCampos = dto.descricaoCampos ?: emptyMap()
            }
            ?: br.com.storehouse.data.entities.ProdutoDescricao(produto = atualizado).apply {
                descricaoCampos = dto.descricaoCampos ?: emptyMap()
            }

        produtoDescricaoRepository.save(descricao)

        logger.info("Produto atualizado: ${produto.codigoBarras} - Filial: ${produto.filial.id}")
        return atualizado
    }

    private fun precisaNovoEstado(estadoAtual: ProdutoEstado?, dto: ProdutoDto): Boolean {
        return estadoAtual == null ||
                estadoAtual.estoque != dto.estoque ||
                estadoAtual.preco != dto.preco
    }

    private fun validarProduto(dto: ProdutoDto, tipoProduto: TipoProduto) {
        if (tipoProduto.campos.isNullOrBlank()) return

        val schema: Map<String, String> = try {
            objectMapper.readValue(tipoProduto.campos, object : TypeReference<Map<String, String>>() {})
        } catch (e: Exception) {
            throw RequisicaoInvalidaException("Formato inválido no campo 'campos' de TipoProduto")
        }

        /**
         * Descomente se necessário para validação de campos obrigatórios
        val descricao = dto.descricaoCampos ?: emptyMap()

        schema.forEach { (campo, _) ->
            val valor = descricao[campo]
            require(!(valor == null || (valor is String && valor.isBlank()))) {
                "Campo obrigatório ausente ou vazio: $campo"
            }
        }
        */
    }

    fun listarTodos(filialId: UUID): List<Produto> =
        produtoRepository.findByFilialIdAndExcluidoFalseOrderByNomeAsc(filialId)

    fun buscarPorCodigo(filialId: UUID, codigo: String): Produto? =
        produtoRepository.findByCodigoBarrasAndFilialIdAndExcluidoFalse(codigo, filialId)

    @LogCall
    @Transactional
    fun atualizarEstoque(filialId: UUID, codigo: String, novoEstoque: Int) {
        val produto = buscarProdutoValido(filialId, codigo)

        produto.estadoAtual?.let {
            if (it.estoque != novoEstoque) {
                it.dataFim = LocalDateTime.now()
                produtoEstadoRepository.save(it)

                val novoEstado = ProdutoEstado(
                    produto = produto,
                    estoque = novoEstoque,
                    preco = it.preco,
                    precoCusto = it.precoCusto
                )
                produtoEstadoRepository.save(novoEstado)
                produto.estadoAtual = novoEstado
                produtoRepository.save(produto)
            }
        }
    }

    @LogCall
    @Transactional
    fun removerLogicamente(filialId: UUID, codigoBarras: String) {
        val produto = buscarProdutoValido(filialId, codigoBarras)
        produto.excluido = true
        produtoRepository.save(produto)
        logger.info("Produto ${produto.codigoBarras} removido logicamente.")
    }

    private fun buscarProdutoValido(filialId: UUID, codigo: String): Produto {
        return produtoRepository.findByCodigoBarrasAndFilialIdAndExcluidoFalse(codigo, filialId)
            ?: throw EntidadeNaoEncontradaException(ErrorMessages.PRODUTO_NAO_ENCONTRADO)
    }
}