package br.com.storehouse.service

import br.com.pinguimice.admin.repository.DespesaRepository
import br.com.pinguimice.admin.service.DespesaService
import br.com.storehouse.data.repository.FilialRepository
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import br.com.storehouse.storage.StorageClientFactory
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.test.util.ReflectionTestUtils
import java.util.UUID

class PrivateAttachmentScopeTest {
    @Test
    fun `missing expense in authenticated filial never reaches storage`() {
        val repository = mock(DespesaRepository::class.java)
        val storage = mock(StorageService::class.java)
        val service = DespesaService(repository, storage, mock(FilialRepository::class.java))
        val expense = UUID.randomUUID()
        val foreignFilial = UUID.randomUUID()
        assertThrows(EntidadeNaoEncontradaException::class.java) { service.obterAnexo(expense, foreignFilial) }
        verify(repository).findByIdAndFilialId(expense, foreignFilial)
        verifyNoInteractions(storage)
    }

    @Test
    fun `JSON supplied attachment cannot point to another expense`() {
        val factory = mock(StorageClientFactory::class.java)
        val service = DefaultStorageService(factory)
        ReflectionTestUtils.setField(service, "despesaBucket", "despesas-files")
        assertThrows(EntidadeNaoEncontradaException::class.java) {
            service.downloadAnexoDespesa(UUID.randomUUID(), "local://despesas-files/despesas/${UUID.randomUUID()}/file.pdf")
        }
        verifyNoInteractions(factory)
    }
}
