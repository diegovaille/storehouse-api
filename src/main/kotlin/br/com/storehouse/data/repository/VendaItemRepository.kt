package br.com.storehouse.data.repository

import br.com.storehouse.data.entities.VendaItem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface VendaItemRepository : JpaRepository<VendaItem, UUID>
