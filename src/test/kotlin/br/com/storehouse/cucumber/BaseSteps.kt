package br.com.storehouse.cucumber

import br.com.pinguimice.admin.repository.*
import br.com.pinguimice.admin.service.*
import br.com.storehouse.data.entities.Usuario
import br.com.storehouse.data.model.UsuarioAutenticado
import br.com.storehouse.data.repository.FilialRepository
import br.com.storehouse.data.repository.OrganizacaoRepository
import br.com.storehouse.data.repository.OrganizacaoUsuarioRepository
import br.com.storehouse.data.repository.PerfilRepository
import br.com.storehouse.data.repository.UsuarioRepository
import br.com.storehouse.service.StorageService
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*

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

    protected var lastException: Exception? = null
}
