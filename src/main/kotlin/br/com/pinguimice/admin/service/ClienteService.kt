package br.com.pinguimice.admin.service

import br.com.pinguimice.admin.entity.Cliente
import br.com.pinguimice.admin.model.ClienteRegiaoInfo
import br.com.pinguimice.admin.model.ClienteRequest
import br.com.pinguimice.admin.model.ClienteResponse
import br.com.pinguimice.admin.repository.ClienteRepository
import br.com.pinguimice.admin.repository.RegiaoVendaRepository
import br.com.storehouse.data.repository.FilialRepository
import br.com.storehouse.exceptions.EntidadeNaoEncontradaException
import br.com.storehouse.exceptions.RequisicaoInvalidaException
import br.com.storehouse.logging.LogCall
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class ClienteService(
    private val clienteRepository: ClienteRepository,
    private val regiaoVendaRepository: RegiaoVendaRepository,
    private val filialRepository: FilialRepository
) {

    @LogCall
    fun listarClientes(filialId: UUID, apenasAtivos: Boolean = false): List<ClienteResponse> {
        val clientes = if (apenasAtivos) {
            clienteRepository.findByFilialIdAndBloqueado(filialId, false)
        } else {
            clienteRepository.findByFilialId(filialId)
        }
        return clientes.map { it.toResponse() }
    }

    @LogCall
    fun buscarPorId(id: UUID, filialId: UUID): ClienteResponse {
        val cliente = clienteRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Cliente não encontrado")

        if (cliente.filial.id != filialId) {
            throw EntidadeNaoEncontradaException("Cliente não pertence à filial informada")
        }

        return cliente.toResponse()
    }

    @LogCall
    @Transactional
    fun criarCliente(request: ClienteRequest, filialId: UUID): ClienteResponse {
        // Validar CNPJ único se informado
        if (!request.cnpj.isNullOrBlank()) {
            val clienteExistente = clienteRepository.findByCnpj(request.cnpj)
            if (clienteExistente != null) {
                throw RequisicaoInvalidaException("Já existe um cliente cadastrado com este CNPJ")
            }
        }

        val filial = filialRepository.findByIdOrNull(filialId)
            ?: throw EntidadeNaoEncontradaException("Filial não encontrada")

        val regiao = request.regiaoId?.let {
            regiaoVendaRepository.findByIdOrNull(it)
                ?: throw EntidadeNaoEncontradaException("Região não encontrada")
        }

        val cliente = Cliente(
            nome = request.nome,
            endereco = request.endereco,
            telefone = request.telefone,
            cnpj = request.cnpj,
            regiao = regiao,
            bloqueado = request.bloqueado,
            motivoBloqueio = request.motivoBloqueio,
            filial = filial
        )

        return clienteRepository.save(cliente).toResponse()
    }

    @LogCall
    @Transactional
    fun atualizarCliente(id: UUID, request: ClienteRequest, filialId: UUID): ClienteResponse {
        val cliente = clienteRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Cliente não encontrado")

        if (cliente.filial.id != filialId) {
            throw EntidadeNaoEncontradaException("Cliente não pertence à filial informada")
        }

        // Validar CNPJ único se informado e mudou
        if (!request.cnpj.isNullOrBlank() && request.cnpj != cliente.cnpj) {
            val clienteExistente = clienteRepository.findByCnpj(request.cnpj)
            if (clienteExistente != null) {
                throw RequisicaoInvalidaException("Já existe um cliente cadastrado com este CNPJ")
            }
        }

        val regiao = request.regiaoId?.let {
            regiaoVendaRepository.findByIdOrNull(it)
                ?: throw EntidadeNaoEncontradaException("Região não encontrada")
        }

        cliente.nome = request.nome
        cliente.endereco = request.endereco
        cliente.telefone = request.telefone
        cliente.cnpj = request.cnpj
        cliente.regiao = regiao
        cliente.bloqueado = request.bloqueado
        cliente.motivoBloqueio = request.motivoBloqueio

        return clienteRepository.save(cliente).toResponse()
    }

    @LogCall
    @Transactional
    fun bloquearCliente(id: UUID, motivo: String, filialId: UUID): ClienteResponse {
        val cliente = clienteRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Cliente não encontrado")

        if (cliente.filial.id != filialId) {
            throw EntidadeNaoEncontradaException("Cliente não pertence à filial informada")
        }

        cliente.bloqueado = true
        cliente.motivoBloqueio = motivo

        return clienteRepository.save(cliente).toResponse()
    }

    @LogCall
    @Transactional
    fun desbloquearCliente(id: UUID, filialId: UUID): ClienteResponse {
        val cliente = clienteRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Cliente não encontrado")

        if (cliente.filial.id != filialId) {
            throw EntidadeNaoEncontradaException("Cliente não pertence à filial informada")
        }

        cliente.bloqueado = false
        cliente.motivoBloqueio = null

        return clienteRepository.save(cliente).toResponse()
    }

    @LogCall
    @Transactional
    fun deletarCliente(id: UUID, filialId: UUID) {
        val cliente = clienteRepository.findByIdOrNull(id)
            ?: throw EntidadeNaoEncontradaException("Cliente não encontrado")

        if (cliente.filial.id != filialId) {
            throw EntidadeNaoEncontradaException("Cliente não pertence à filial informada")
        }

        clienteRepository.delete(cliente)
    }

    private fun Cliente.toResponse() = ClienteResponse(
        id = this.id,
        nome = this.nome,
        endereco = this.endereco,
        telefone = this.telefone,
        cnpj = this.cnpj,
        regiao = this.regiao?.let {
            ClienteRegiaoInfo(
                id = it.id,
                nome = it.nome
            )
        },
        bloqueado = this.bloqueado,
        motivoBloqueio = this.motivoBloqueio,
        dataCriacao = this.dataCriacao
    )
}

