package br.com.pinguimice.admin.repository

import br.com.pinguimice.admin.entity.PinguimVendaItem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface PinguimVendaItemRepository : JpaRepository<PinguimVendaItem, UUID>
