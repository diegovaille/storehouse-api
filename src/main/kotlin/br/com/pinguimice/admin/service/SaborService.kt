package br.com.pinguimice.admin.service

import br.com.pinguimice.admin.entity.Sabor
import br.com.pinguimice.admin.model.SaborRequest
import br.com.pinguimice.admin.model.SaborResponse
import br.com.pinguimice.admin.repository.SaborRepository
import br.com.storehouse.data.repository.FilialRepository
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import br.com.storehouse.logging.LogCall
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class SaborService(
    private val saborRepository: SaborRepository,
    private val filialRepository: FilialRepository
) {

    @LogCall
    fun listarSabores(apenasAtivos: Boolean = true): List<SaborResponse> {
        val sabores = if (apenasAtivos) {
            saborRepository.findByAtivoTrue()
        } else {
            saborRepository.findAll()
        }
        return sabores.map { it.toResponse() }
    }

    @LogCall
    @Transactional
    fun criarSabor(request: SaborRequest, filialId: UUID): SaborResponse {
        val filial = filialRepository.findById(filialId)
            .orElseThrow { EntidadeNaoEncontradaException("Filial não encontrada") }

        val sabor = Sabor(
            nome = request.nome,
            corHex = request.corHex,
            usaAcucar = request.usaAcucar ?: false,
            filial = filial
        )

        return saborRepository.save(sabor).toResponse()
    }

    @LogCall
    @Transactional
    fun editarSabor(id: UUID, request: SaborRequest, filialId: UUID): SaborResponse {
        val sabor = saborRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Sabor não encontrado")

        if (sabor.filial.id != filialId) {
            // Não vaza informação de outra filial
            throw EntidadeNaoEncontradaException("Sabor não encontrado")
        }

        sabor.nome = request.nome
        sabor.corHex = request.corHex
        sabor.usaAcucar = request.usaAcucar ?: sabor.usaAcucar

        return saborRepository.save(sabor).toResponse()
    }
}
