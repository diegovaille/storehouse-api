package br.com.storehouse.cucumber

import br.com.pinguimice.admin.repository.*
import br.com.pinguimice.admin.service.*
import br.com.storehouse.data.repository.*
import br.com.storehouse.service.StorageService
import org.springframework.beans.factory.annotation.Autowired

open class BaseSteps {

    @Autowired protected lateinit var saborService: SaborService
    @Autowired protected lateinit var regiaoVendaService: RegiaoVendaService
    @Autowired protected lateinit var estoqueService: EstoqueService
    @Autowired protected lateinit var producaoService: ProducaoService
    @Autowired protected lateinit var despesaService: DespesaService
    @Autowired protected lateinit var storageService: StorageService
    
    @Autowired protected lateinit var saborRepository: SaborRepository
    @Autowired protected lateinit var regiaoVendaRepository: RegiaoVendaRepository
    @Autowired protected lateinit var materiaPrimaRepository: MateriaPrimaRepository
    @Autowired protected lateinit var embalagemRepository: EmbalagemRepository
    @Autowired protected lateinit var outrosRepository: OutrosRepository
    @Autowired protected lateinit var producaoRepository: ProducaoRepository
    @Autowired protected lateinit var estoqueGelinhoRepository: EstoqueGelinhoRepository
    @Autowired protected lateinit var usuarioRepository: UsuarioRepository
    @Autowired protected lateinit var organizacaoUsuarioRepository: OrganizacaoUsuarioRepository
    @Autowired protected lateinit var filialRepository: FilialRepository
    @Autowired protected lateinit var organizacaoRepository: OrganizacaoRepository
    @Autowired protected lateinit var perfilRepository: PerfilRepository
    @Autowired protected lateinit var vendaRepository: PinguimVendaRepository
    @Autowired protected lateinit var vendaItemRepository: PinguimVendaItemRepository

    @Autowired protected lateinit var clienteRepository: ClienteRepository
    @Autowired protected lateinit var despesaRepository: DespesaRepository

    protected var lastException: Exception? = null
}
