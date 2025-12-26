package br.com.pinguimice.admin.repository

import br.com.pinguimice.admin.entity.Outros
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface OutrosRepository : JpaRepository<Outros, UUID> {
    fun findByFilialIdOrderByDataCriacaoDesc(filialId: UUID): List<Outros>
    fun findByIdAndFilialId(id: UUID, filialId: UUID): Outros?

    fun findByFilialIdAndNome(filialId: UUID, nome: String): Outros?
    fun findByFilialIdAndNomeContainingIgnoreCaseOrderByDataCriacaoAsc(filialId: UUID, nome: String): List<Outros>
}
