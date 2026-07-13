package br.com.storehouse.data.repository

import br.com.storehouse.data.entities.Produto
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface ProdutoRepository : JpaRepository<Produto, UUID> {
    fun findByFilialIdAndExcluidoFalseOrderByNomeAsc(filialId: UUID): List<Produto>
    fun findByCodigoBarrasAndFilialIdAndExcluidoFalse(codigoBarras: String, filialId: UUID): Produto?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): Produto?
}
