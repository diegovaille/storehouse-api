package br.com.pinguimice.admin.service

import br.com.pinguimice.admin.entity.RegiaoVenda
import br.com.pinguimice.admin.model.RegiaoVendaRequest
import br.com.pinguimice.admin.model.RegiaoVendaResponse
import br.com.pinguimice.admin.repository.RegiaoVendaRepository
import br.com.storehouse.data.repository.FilialRepository
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import br.com.storehouse.logging.LogCall
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class RegiaoVendaService(
    private val regiaoVendaRepository: RegiaoVendaRepository,
    private val filialRepository: FilialRepository
) {

    @LogCall
    fun listarRegioes(apenasAtivos: Boolean = true): List<RegiaoVendaResponse> {
        val regioes = if (apenasAtivos) {
            regiaoVendaRepository.findByAtivoTrue()
        } else {
            regiaoVendaRepository.findAll()
        }
        return regioes.map { it.toResponse() }
    }

    @LogCall
    @Transactional
    fun criarRegiao(request: RegiaoVendaRequest, filialId: UUID): RegiaoVendaResponse {
        val filial = filialRepository.findById(filialId)
            .orElseThrow { EntidadeNaoEncontradaException("Filial não encontrada") }

        val regiao = RegiaoVenda(
            nome = request.nome,
            descricao = request.descricao,
            filial = filial
        )

        return regiaoVendaRepository.save(regiao).toResponse()
    }

    @LogCall
    @Transactional
    fun atualizarRegiao(id: UUID, request: RegiaoVendaRequest): RegiaoVendaResponse {
        val regiao = regiaoVendaRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Região não encontrada")

        regiao.nome = request.nome
        regiao.descricao = request.descricao

        return regiaoVendaRepository.save(regiao).toResponse()
    }
}
