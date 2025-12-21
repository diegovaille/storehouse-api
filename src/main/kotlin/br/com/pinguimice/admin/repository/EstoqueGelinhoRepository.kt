package br.com.pinguimice.admin.repository

import br.com.pinguimice.admin.entity.EstoqueGelinho
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface EstoqueGelinhoRepository : JpaRepository<EstoqueGelinho, UUID> {
    fun findByFilialIdOrderByUltimaAtualizacaoDesc(filialId: UUID): List<EstoqueGelinho>
    fun findBySaborIdAndFilialId(saborId: UUID, filialId: UUID): EstoqueGelinho?

    // Mantido para usos internos existentes (preferir a versão com filialId)
    fun findBySaborId(saborId: UUID): EstoqueGelinho?
}
