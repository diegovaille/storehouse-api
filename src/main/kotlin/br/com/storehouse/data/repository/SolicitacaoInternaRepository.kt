package br.com.storehouse.data.repository

import br.com.storehouse.data.entities.SolicitacaoInterna
import br.com.storehouse.data.enums.StatusSolicitacaoInterna
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface SolicitacaoInternaRepository : JpaRepository<SolicitacaoInterna, UUID> {
    fun findByFilialIdOrderByDataCriacaoDesc(filialId: UUID): List<SolicitacaoInterna>
    fun findByFilialIdAndStatusInOrderByDataCriacaoDesc(
        filialId: UUID, status: Collection<StatusSolicitacaoInterna>
    ): List<SolicitacaoInterna>
}
