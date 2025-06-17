package br.com.storehouse.data.repository

import br.com.storehouse.data.entities.VendaPagamento
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface VendaPagamentoRepository : JpaRepository<VendaPagamento, UUID>