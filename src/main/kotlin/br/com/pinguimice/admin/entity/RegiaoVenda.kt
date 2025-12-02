package br.com.pinguimice.admin.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "regiao_venda", schema = "pinguim")
class RegiaoVenda(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 100)
    var nome: String,

    @Column(columnDefinition = "TEXT")
    var descricao: String? = null,

    @Column(nullable = false)
    var ativo: Boolean = true,

    @Column(name = "data_criacao", nullable = false)
    var dataCriacao: LocalDateTime = LocalDateTime.now()
)
