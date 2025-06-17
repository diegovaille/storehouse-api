package br.com.storehouse.data.repository

import br.com.storehouse.data.entities.Filial
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface FilialRepository : JpaRepository<Filial, UUID>