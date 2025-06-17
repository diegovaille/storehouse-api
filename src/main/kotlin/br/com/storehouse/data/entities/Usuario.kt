package br.com.storehouse.data.entities

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.*

@Entity
class Usuario(
    @Id
    var id: UUID = UUID.randomUUID(),

    var username: String? = null,
    var password: String? = null,
    var email: String = "",
)
