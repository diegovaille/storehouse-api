package br.com.storehouse.data.repository

import br.com.storehouse.data.entities.Organizacao
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface OrganizacaoRepository : JpaRepository<Organizacao, UUID>