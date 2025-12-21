package br.com.pinguimice.admin.service

import br.com.pinguimice.admin.entity.ParametroCalculo
import br.com.pinguimice.admin.model.ParametroCalculoRequest
import br.com.pinguimice.admin.model.ParametroCalculoResponse
import br.com.pinguimice.admin.repository.ParametroCalculoRepository
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import br.com.storehouse.logging.LogCall
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ParametroCalculoService(
    private val parametroRepository: ParametroCalculoRepository
) {

    companion object {
        // Chaves dos parâmetros
        const val UNIDADES_POR_EMBALAGEM_KG = "UNIDADES_POR_EMBALAGEM_KG"
        const val TOTAL_PACOTES_POR_CAIXA = "TOTAL_PACOTES_POR_CAIXA"
        const val TOTAL_UNIDADE_POR_KG = "TOTAL_UNIDADE_POR_KG"
        const val TOTAL_UNIDADE_ACUCAR_POR_KG = "TOTAL_UNIDADE_ACUCAR_POR_KG"
        const val UNIDADES_POR_PACOTE = "UNIDADES_POR_PACOTE"
    }

    @LogCall
    @Cacheable("parametros")
    fun listarParametros(): List<ParametroCalculoResponse> {
        return parametroRepository.findAll().map { it.toResponse() }
    }

    @LogCall
    @Cacheable("parametros", key = "#chave")
    fun buscarPorChave(chave: String): ParametroCalculoResponse {
        val parametro = parametroRepository.findByChave(chave)
            ?: throw EntidadeNaoEncontradaException("Parâmetro '$chave' não encontrado")
        return parametro.toResponse()
    }

    @LogCall
    @Cacheable("parametros", key = "#chave")
    fun obterValor(chave: String): Double {
        return parametroRepository.findByChave(chave)?.valor
            ?: throw EntidadeNaoEncontradaException("Parâmetro '$chave' não encontrado")
    }

    @LogCall
    @Transactional
    @CacheEvict(value = ["parametros"], allEntries = true)
    fun atualizarParametro(chave: String, request: ParametroCalculoRequest): ParametroCalculoResponse {
        val parametro = parametroRepository.findByChave(chave)
            ?: throw EntidadeNaoEncontradaException("Parâmetro '$chave' não encontrado")

        parametro.valor = request.valor
        parametro.descricao = request.descricao
        parametro.dataAtualizacao = LocalDateTime.now()

        return parametroRepository.save(parametro).toResponse()
    }

    @LogCall
    @Transactional
    @CacheEvict(value = ["parametros"], allEntries = true)
    fun criarParametro(request: ParametroCalculoRequest): ParametroCalculoResponse {
        val parametroExistente = parametroRepository.findByChave(request.chave)
        if (parametroExistente != null) {
            throw IllegalArgumentException("Parâmetro '${request.chave}' já existe")
        }

        val parametro = ParametroCalculo(
            chave = request.chave,
            valor = request.valor,
            descricao = request.descricao
        )

        return parametroRepository.save(parametro).toResponse()
    }

    private fun ParametroCalculo.toResponse() = ParametroCalculoResponse(
        chave = this.chave,
        valor = this.valor,
        descricao = this.descricao,
        dataAtualizacao = this.dataAtualizacao
    )
}

