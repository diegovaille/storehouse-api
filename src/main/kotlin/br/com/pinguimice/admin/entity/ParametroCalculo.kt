package br.com.pinguimice.admin.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "parametro_calculo", schema = "pinguim")
class ParametroCalculo(
    @Id
    @Column(nullable = false, unique = true, length = 100)
    var chave: String,

    @Column(nullable = false)
    var valor: Double,

    @Column(length = 255)
    var descricao: String? = null,

    @Column(name = "data_atualizacao", nullable = false)
    var dataAtualizacao: LocalDateTime = LocalDateTime.now()
)

