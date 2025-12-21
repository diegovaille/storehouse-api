package br.com.pinguimice.admin.service

import br.com.pinguimice.admin.entity.PinguimVenda
import br.com.pinguimice.admin.entity.PinguimVendaItem
import br.com.pinguimice.admin.entity.Sabor
import br.com.pinguimice.admin.model.PinguimVendaItemRequest
import br.com.pinguimice.admin.model.PinguimVendaRequest
import br.com.pinguimice.admin.repository.ClienteRepository
import br.com.pinguimice.admin.repository.EstoqueGelinhoRepository
import br.com.pinguimice.admin.repository.PinguimVendaRepository
import br.com.pinguimice.admin.repository.SaborRepository
import br.com.storehouse.data.entities.Filial
import br.com.storehouse.data.repository.UsuarioRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class PinguimVendaServiceOrphanRemovalTest {

    @Test
    fun `editarVenda deve atualizar itens in-place para evitar erro de orphanRemoval`() {
        val filialId = UUID.randomUUID()
        val vendaId = UUID.randomUUID()
        val vendedorEmail = "admin@x.com"

        val vendaRepo = Mockito.mock(PinguimVendaRepository::class.java)
        val saborRepo = Mockito.mock(SaborRepository::class.java)
        val estoqueRepo = Mockito.mock(EstoqueGelinhoRepository::class.java)
        val usuarioRepo = Mockito.mock(UsuarioRepository::class.java)
        val clienteRepo = Mockito.mock(ClienteRepository::class.java)
        val filialProvider = Mockito.mock(FilialProvider::class.java)

        val usuarioMock = Mockito.mock(br.com.storehouse.data.entities.Usuario::class.java)
        `when`(usuarioMock.username).thenReturn("Vendedor")
        `when`(usuarioRepo.findByEmail(vendedorEmail)).thenReturn(usuarioMock)

        val filial = Filial(id = filialId, nome = "Filial", organizacao = Mockito.mock(br.com.storehouse.data.entities.Organizacao::class.java))
        `when`(filialProvider.get(filialId)).thenReturn(filial)

        val sabor1 = Sabor(nome = "Morango", corHex = "#FF0000", filial = filial)
        val sabor2 = Sabor(nome = "Uva", corHex = "#9900FF", filial = filial)
        `when`(saborRepo.findById(sabor1.id)).thenReturn(Optional.of(sabor1))
        `when`(saborRepo.findById(sabor2.id)).thenReturn(Optional.of(sabor2))

        val vendaExistente = PinguimVenda(
            id = vendaId,
            total = BigDecimal("10.00"),
            totalPago = BigDecimal("0.00"),
            usuarioId = UUID.randomUUID(),
            abaterEstoque = false,
            filial = filial,
            itens = mutableListOf()
        )
        vendaExistente.itens.add(
            PinguimVendaItem(venda = vendaExistente, sabor = sabor1, quantidade = 1)
        )
        val itensAntes = vendaExistente.itens

        `when`(vendaRepo.findByIdAndFilialId(vendaId, filialId)).thenReturn(vendaExistente)
        `when`(vendaRepo.save(any(PinguimVenda::class.java))).thenAnswer { it.arguments[0] as PinguimVenda }

        val service = PinguimVendaService(
            vendaRepository = vendaRepo,
            saborRepository = saborRepo,
            estoqueRepository = estoqueRepo,
            usuarioRepository = usuarioRepo,
            clienteRepository = clienteRepo,
            filialProvider = filialProvider
        )

        val request = PinguimVendaRequest(
            clienteId = null,
            itens = listOf(
                PinguimVendaItemRequest(saborId = sabor2.id, quantidade = 2)
            ),
            total = BigDecimal("20.00"),
            totalPago = BigDecimal("5.00"),
            abaterEstoque = false
        )

        service.editarVenda(vendaId, request, filialId, vendedorEmail)

        // Mesma instância de coleção (não foi reatribuída)
        assertSame(itensAntes, vendaExistente.itens)

        assertEquals(1, vendaExistente.itens.size)
        assertEquals(sabor2.id, vendaExistente.itens[0].sabor.id)
        assertEquals(2, vendaExistente.itens[0].quantidade)

        verify(vendaRepo).save(vendaExistente)
    }
}
