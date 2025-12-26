package br.com.pinguimice.admin.repository

import br.com.pinguimice.admin.entity.Embalagem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface EmbalagemRepository : JpaRepository<Embalagem, UUID> {
    fun findByFilialIdOrderByDataCriacaoDesc(filialId: UUID): List<Embalagem>
    fun findByIdAndFilialId(id: UUID, filialId: UUID): Embalagem?

    fun findByFilialIdAndSaborIdOrderByDataCriacaoAsc(filialId: UUID, saborId: UUID): List<Embalagem>
    fun findByFilialIdAndSaborIdIsNull(filialId: UUID): List<Embalagem>
}
