package br.com.pinguimice.admin.repository

import br.com.pinguimice.admin.entity.ParametroCalculo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ParametroCalculoRepository : JpaRepository<ParametroCalculo, String> {
    fun findByChave(chave: String): ParametroCalculo?
}

