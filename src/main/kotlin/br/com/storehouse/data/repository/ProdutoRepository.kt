package br.com.storehouse.data.repository

import br.com.storehouse.data.entities.Produto
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ProdutoRepository : JpaRepository<Produto, UUID> {
    fun findByFilialIdAndExcluidoFalseOrderByNomeAsc(filialId: UUID): List<Produto>
    fun findByCodigoBarrasAndFilialIdAndExcluidoFalse(codigoBarras: String, filialId: UUID): Produto?
}
