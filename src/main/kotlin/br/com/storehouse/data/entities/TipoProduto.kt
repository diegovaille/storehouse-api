package br.com.storehouse.data.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "tipo_produto")
class TipoProduto(
    @Id
    var id: UUID = UUID.randomUUID(),

    var nome: String = "",

    @Column(columnDefinition = "jsonb")
    var campos: String = "{}"
)