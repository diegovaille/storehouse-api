package br.com.storehouse.data.repository

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ProdutoDescricaoRepository : JpaRepository<br.com.storehouse.data.entities.ProdutoDescricao, UUID> {
    fun findByProdutoId(produtoId: UUID): br.com.storehouse.data.entities.ProdutoDescricao?
}
