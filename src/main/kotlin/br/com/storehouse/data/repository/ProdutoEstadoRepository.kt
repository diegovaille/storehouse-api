package br.com.storehouse.data.repository

import br.com.storehouse.data.entities.ProdutoEstado
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ProdutoEstadoRepository : JpaRepository<ProdutoEstado, UUID>