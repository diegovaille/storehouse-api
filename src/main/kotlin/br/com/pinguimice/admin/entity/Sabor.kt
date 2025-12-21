package br.com.pinguimice.admin.entity

import br.com.storehouse.data.entities.Filial
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

    @Column(name = "usa_acucar", nullable = false)
    var usaAcucar: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filial_id", nullable = false)
    var filial: Filial,

    @Column(name = "data_criacao", nullable = false)
    var dataCriacao: LocalDateTime = LocalDateTime.now()
)
