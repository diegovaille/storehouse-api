package br.com.storehouse.data.repository

import br.com.storehouse.data.entities.TipoProduto
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface TipoProdutoRepository : JpaRepository<TipoProduto, UUID>