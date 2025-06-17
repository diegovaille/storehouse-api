package br.com.storehouse.data.entities

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.*

@Entity
@Table(name = "produto_descricao")
class ProdutoDescricao(
    @Id
    var id: UUID = UUID.randomUUID(),

    @OneToOne
    @JoinColumn(name = "produto_id", nullable = false, unique = true)
    var produto: br.com.storehouse.data.entities.Produto,

    @Column(name = "descricao_campos",columnDefinition = "jsonb")
//    @Convert(converter = JsonMapConverter::class)
    @JdbcTypeCode(SqlTypes.JSON)
    var descricaoCampos: Map<String, Any> = emptyMap()

)