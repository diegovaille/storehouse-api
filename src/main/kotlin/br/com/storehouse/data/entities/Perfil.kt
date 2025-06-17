package br.com.storehouse.data.entities

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.*

@Entity
class Perfil(
    @Id
    var id: UUID = UUID.randomUUID(),
    var tipo: String = "VENDEDOR" // ADMIN ou VENDEDOR
)
