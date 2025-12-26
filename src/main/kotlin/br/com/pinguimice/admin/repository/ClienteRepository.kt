package br.com.pinguimice.admin.repository

import br.com.pinguimice.admin.entity.Cliente
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ClienteRepository : JpaRepository<Cliente, UUID> {
    fun findByFilialId(filialId: UUID): List<Cliente>
    fun findByFilialIdAndBloqueado(filialId: UUID, bloqueado: Boolean): List<Cliente>
    fun findByCnpj(cnpj: String): Cliente?
}

