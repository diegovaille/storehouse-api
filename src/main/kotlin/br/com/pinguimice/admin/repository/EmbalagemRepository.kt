package br.com.pinguimice.admin.repository

import br.com.pinguimice.admin.entity.Embalagem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface EmbalagemRepository : JpaRepository<Embalagem, UUID> {
    fun findBySaborIdOrderByDataCriacaoAsc(saborId: UUID): List<Embalagem>
    fun findBySaborIdIsNull(): List<Embalagem>
}
