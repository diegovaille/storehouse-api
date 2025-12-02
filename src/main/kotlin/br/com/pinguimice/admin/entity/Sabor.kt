package br.com.pinguimice.admin.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "sabor", schema = "pinguim")
class Sabor(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 100)
    var nome: String,

    @Column(name = "cor_hex", length = 7)
    var corHex: String? = null,

    @Column(nullable = false)
    var ativo: Boolean = true,

    @Column(name = "data_criacao", nullable = false)
    var dataCriacao: LocalDateTime = LocalDateTime.now()
)
