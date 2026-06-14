package br.com.storehouse.data.repository

import br.com.storehouse.data.entities.Solicitacao
import br.com.storehouse.data.enums.StatusSolicitacao
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface SolicitacaoRepository : JpaRepository<Solicitacao, UUID> {
    fun findByFilialIdOrderByDataCriacaoDesc(filialId: UUID): List<Solicitacao>
    fun findByFilialIdAndStatusInOrderByDataCriacaoDesc(
        filialId: UUID, status: Collection<StatusSolicitacao>
    ): List<Solicitacao>
}
