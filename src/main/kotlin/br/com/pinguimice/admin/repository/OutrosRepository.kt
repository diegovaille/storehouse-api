package br.com.pinguimice.admin.repository

import br.com.pinguimice.admin.entity.Outros
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface OutrosRepository : JpaRepository<Outros, UUID> {
    fun findByNome(nome: String): Outros?
    fun findByNomeContainingIgnoreCaseOrderByDataCriacaoAsc(nome: String): List<Outros>
}
