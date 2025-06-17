package br.com.storehouse.service

import br.com.storehouse.data.repository.TipoProdutoRepository
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import org.springframework.stereotype.Service
import java.util.*

@Service
class TipoProdutoService(
    private val tipoProdutoRepository: TipoProdutoRepository
) {
    fun listarTodos(): List<br.com.storehouse.data.entities.TipoProduto> = tipoProdutoRepository.findAll()

    fun buscarPorId(id: UUID): br.com.storehouse.data.entities.TipoProduto =
        tipoProdutoRepository.findById(id)
            .orElseThrow { EntidadeNaoEncontradaException("Tipo de produto não encontrado") }
}