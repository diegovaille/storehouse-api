package br.com.pinguimice.admin.service

import br.com.pinguimice.admin.entity.Sabor
import br.com.pinguimice.admin.model.SaborRequest
import br.com.pinguimice.admin.model.SaborResponse
import br.com.pinguimice.admin.repository.SaborRepository
import br.com.storehouse.logging.LogCall
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SaborService(
    private val saborRepository: SaborRepository
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
    fun criarSabor(request: SaborRequest): SaborResponse {
        val sabor = Sabor(
            nome = request.nome,
            corHex = request.corHex
        )

        return saborRepository.save(sabor).toResponse()
    }
}
