package br.com.storehouse.service

import br.com.storehouse.data.entities.Filial
import br.com.storehouse.data.entities.Solicitacao
import br.com.storehouse.data.enums.StatusSolicitacao
import br.com.storehouse.data.model.SolicitacaoRequest
import br.com.storehouse.data.model.SolicitacaoUpdateRequest
import br.com.storehouse.data.repository.FilialRepository
import br.com.storehouse.data.repository.SolicitacaoRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.*

class SolicitacaoServiceTest {

    private val repo: SolicitacaoRepository = Mockito.mock(SolicitacaoRepository::class.java).also { r ->
        Mockito.`when`(r.save(Mockito.any(Solicitacao::class.java))).thenAnswer { it.arguments[0] as Solicitacao }
    }
    private val filialRepo: FilialRepository = Mockito.mock(FilialRepository::class.java)
    private val service = SolicitacaoService(repo, filialRepo)

    private val filialId = UUID.randomUUID()

    private fun mockFilial() {
        val filial = Mockito.mock(Filial::class.java)
        Mockito.`when`(filialRepo.findById(filialId)).thenReturn(Optional.of(filial))
    }

    @Test
    fun `criar inicia com status SOLICITADO`() {
        mockFilial()
        val req = SolicitacaoRequest(
            descricaoItem = "Livro: Biblia NVI", categoria = "Livro",
            nomeSolicitante = "Maria", contato = "11999998888"
        )
        val resp = service.criar(filialId, req)
        assertEquals(StatusSolicitacao.SOLICITADO, resp.status)
        assertEquals("Maria", resp.nomeSolicitante)
        assertNotNull(resp.id)
    }

    @Test
    fun `atualizar muda o status`() {
        val filialMock = Mockito.mock(Filial::class.java)
        Mockito.`when`(filialMock.id).thenReturn(filialId)
        val sol = Solicitacao(
            filial = filialMock,
            descricaoItem = "Camiseta", nomeSolicitante = "Joao", contato = "11"
        )
        Mockito.`when`(repo.findById(sol.id)).thenReturn(Optional.of(sol))
        val resp = service.atualizar(filialId, sol.id.toString(), SolicitacaoUpdateRequest(status = StatusSolicitacao.SEPARADO))
        assertEquals(StatusSolicitacao.SEPARADO, resp.status)
    }

    @Test
    fun `atualizar com notificar grava notificadoEm`() {
        val filialMock = Mockito.mock(Filial::class.java)
        Mockito.`when`(filialMock.id).thenReturn(filialId)
        val sol = Solicitacao(
            filial = filialMock,
            descricaoItem = "Bone", nomeSolicitante = "Ana", contato = "11"
        )
        Mockito.`when`(repo.findById(sol.id)).thenReturn(Optional.of(sol))
        val resp = service.atualizar(filialId, sol.id.toString(), SolicitacaoUpdateRequest(notificar = true))
        assertNotNull(resp.notificadoEm)
    }
}
