package br.com.storehouse.cucumber

import br.com.pinguimice.admin.entity.TipoEntrada
import br.com.pinguimice.admin.model.*
import br.com.pinguimice.admin.repository.*
import br.com.pinguimice.admin.service.*
import br.com.storehouse.data.entities.Usuario
import br.com.storehouse.data.model.UsuarioAutenticado
import br.com.storehouse.data.repository.FilialRepository
import br.com.storehouse.data.repository.OrganizacaoUsuarioRepository
import br.com.storehouse.data.repository.UsuarioRepository
import br.com.storehouse.service.StorageService
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class PinguimiceSteps {

    @Autowired private lateinit var saborService: SaborService
    @Autowired private lateinit var regiaoVendaService: RegiaoVendaService
    @Autowired private lateinit var estoqueService: EstoqueService
    @Autowired private lateinit var producaoService: ProducaoService
    @Autowired private lateinit var despesaService: DespesaService
    @Autowired private lateinit var storageService: StorageService
    
    @Autowired private lateinit var saborRepository: SaborRepository
    @Autowired private lateinit var regiaoVendaRepository: RegiaoVendaRepository
    @Autowired private lateinit var materiaPrimaRepository: MateriaPrimaRepository
    @Autowired private lateinit var embalagemRepository: EmbalagemRepository
    @Autowired private lateinit var outrosRepository: OutrosRepository
    @Autowired private lateinit var producaoRepository: ProducaoRepository
    @Autowired private lateinit var estoqueGelinhoRepository: EstoqueGelinhoRepository
    @Autowired private lateinit var usuarioRepository: UsuarioRepository
    @Autowired private lateinit var organizacaoUsuarioRepository: OrganizacaoUsuarioRepository
    @Autowired private lateinit var filialRepository: FilialRepository


    private var lastSaborResponse: SaborResponse? = null
    private var lastRegiaoResponse: RegiaoVendaResponse? = null
    private var lastMateriaPrimaResponse: MateriaPrimaResponse? = null
    private var lastEmbalagemResponse: EmbalagemResponse? = null
    private var lastOutrosResponse: OutrosResponse? = null
    private var lastDespesaResponse: DespesaResponse? = null
    private var lastException: Exception? = null

    @Before
    fun setup() {
        producaoRepository.deleteAll()
        estoqueGelinhoRepository.deleteAll()
        materiaPrimaRepository.deleteAll()
        embalagemRepository.deleteAll()
        outrosRepository.deleteAll()
        saborRepository.deleteAll()
        regiaoVendaRepository.deleteAll()
        organizacaoUsuarioRepository.deleteAll()
        filialRepository.deleteAll()
        usuarioRepository.deleteAll()
    }

    // Common
    @Given("que eu sou um usuário autenticado")
    fun que_eu_sou_um_usuario_autenticado() {
        val usuario = Usuario(
            username = "Test User",
            email = "test@pinguimice.com.br",
            password = "password"
        )
        usuarioRepository.save(usuario)

        val userDetails = UsuarioAutenticado(
            email = usuario.email,
            perfil = "ADMIN",
            organizacaoId = UUID.randomUUID(),
            filialId = UUID.randomUUID()
        )
        val auth = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
        SecurityContextHolder.getContext().authentication = auth
    }

    // Sabor
    @When("eu crio um sabor com nome {string} e cor {string}")
    fun eu_crio_um_sabor(nome: String, cor: String) {
        val request = SaborRequest(nome = nome, corHex = cor)
        lastSaborResponse = saborService.criarSabor(request)
    }

    @Then("o sabor {string} deve ser listado com sucesso")
    fun o_sabor_deve_ser_listado(nome: String) {
        val sabores = saborService.listarSabores()
        assertTrue(sabores.any { it.nome == nome })
    }

    @Given("que existe um sabor {string} ativo")
    fun que_existe_um_sabor_ativo(nome: String) {
        saborService.criarSabor(SaborRequest(nome = nome, corHex = "#000000"))
    }

    @Given("que existe um sabor {string} inativo")
    fun que_existe_um_sabor_inativo(nome: String) {
        val sabor = saborService.criarSabor(SaborRequest(nome = nome, corHex = "#000000"))
        val entity = saborRepository.findById(sabor.id).get()
        entity.ativo = false
        saborRepository.save(entity)
    }

    @When("eu listo os sabores ativos")
    fun eu_listo_os_sabores_ativos() {
        // Just calling the service, verification is in Then
    }

    @Then("eu devo ver o sabor {string}")
    fun eu_devo_ver_o_sabor(nome: String) {
        val sabores = saborService.listarSabores(apenasAtivos = true)
        assertTrue(sabores.any { it.nome == nome })
    }

    @Then("eu não devo ver o sabor {string}")
    fun eu_nao_devo_ver_o_sabor(nome: String) {
        val sabores = saborService.listarSabores(apenasAtivos = true)
        assertFalse(sabores.any { it.nome == nome })
    }

    // Regiao
    @When("eu crio uma região com nome {string}")
    fun eu_crio_uma_regiao(nome: String) {
        lastRegiaoResponse = regiaoVendaService.criarRegiao(RegiaoVendaRequest(nome = nome))
    }

    @Then("a região {string} deve ser listada com sucesso")
    fun a_regiao_deve_ser_listada(nome: String) {
        val regioes = regiaoVendaService.listarRegioes()
        assertTrue(regioes.any { it.nome == nome })
    }

    @Given("que existe uma região {string}")
    fun que_existe_uma_regiao(nome: String) {
        lastRegiaoResponse = regiaoVendaService.criarRegiao(RegiaoVendaRequest(nome = nome))
    }

    @When("eu atualizo o nome da região {string} para {string}")
    fun eu_atualizo_a_regiao(nomeAntigo: String, nomeNovo: String) {
        val regiao = regiaoVendaRepository.findAll().first { it.nome == nomeAntigo }
        lastRegiaoResponse = regiaoVendaService.atualizarRegiao(regiao.id, RegiaoVendaRequest(nome = nomeNovo))
    }

    @Then("a região deve ser atualizada para {string}")
    fun a_regiao_deve_ser_atualizada(nome: String) {
        assertEquals(nome, lastRegiaoResponse?.nome)
    }

    // Estoque
    @Given("que existe um sabor {string}")
    fun que_existe_um_sabor(nome: String) {
        lastSaborResponse = saborService.criarSabor(SaborRequest(nome = nome, corHex = "#FFFFFF"))
    }

    @When("eu adiciono estoque de matéria prima {string} para o sabor {string} do tipo {string} com quantidade {double} e preço {double}")
    fun eu_adiciono_estoque_materia_prima(nomeMp: String, nomeSabor: String, tipo: String, qtd: Double, preco: Double) {
        val sabor = saborRepository.findAll().first { it.nome == nomeSabor }
        lastMateriaPrimaResponse = estoqueService.criarMateriaPrima(
            MateriaPrimaRequest(
                nome = nomeMp,
                saborId = sabor.id,
                tipoEntrada = TipoEntrada.valueOf(tipo),
                quantidadeEntrada = BigDecimal.valueOf(qtd),
                precoEntrada = BigDecimal.valueOf(preco)
            )
        )
    }

    @Then("o sistema deve calcular {int} unidades totais")
    fun o_sistema_deve_calcular_unidades(unidades: Int) {
        if (lastMateriaPrimaResponse != null) {
            assertEquals(unidades, lastMateriaPrimaResponse!!.totalUnidades)
        } else if (lastEmbalagemResponse != null) {
            assertEquals(unidades, lastEmbalagemResponse!!.totalUnidades)
        } else if (lastOutrosResponse != null) {
            assertEquals(unidades, lastOutrosResponse!!.totalUnidades)
        } else {
            fail("Nenhuma resposta de estoque encontrada")
        }
    }

    @Then("o preço por unidade deve ser aproximadamente {double}")
    fun o_preco_por_unidade_deve_ser(preco: Double) {
        val actual = lastMateriaPrimaResponse?.precoPorUnidade 
            ?: lastEmbalagemResponse?.precoPorUnidade 
            ?: lastOutrosResponse?.precoPorUnidade
            ?: fail("Nenhuma resposta encontrada")
            
        assertEquals(preco, actual.toDouble(), 0.0001)
    }

    @When("eu adiciono estoque de embalagem {string} para o sabor {string} com {double} kg e preço {double}")
    fun eu_adiciono_estoque_embalagem(nomeEmb: String, nomeSabor: String, qtd: Double, preco: Double) {
        val sabor = saborRepository.findAll().first { it.nome == nomeSabor }
        lastEmbalagemResponse = estoqueService.criarEmbalagem(
            EmbalagemRequest(
                nome = nomeEmb,
                saborId = sabor.id,
                quantidadeKg = BigDecimal.valueOf(qtd),
                precoKg = BigDecimal.valueOf(preco)
            )
        )
    }

    @When("eu adiciono estoque de outros {string} com quantidade {int}, preço {double} e unidades por item {int}")
    fun eu_adiciono_estoque_outros(nome: String, qtd: Int, preco: Double, unidadesPorItem: Int) {
        lastOutrosResponse = estoqueService.criarOutros(
            OutrosRequest(
                nome = nome,
                quantidadeEntrada = qtd,
                precoEntrada = BigDecimal.valueOf(preco),
                unidadesPorItem = unidadesPorItem
            )
        )
    }

    // Producao
    @Given("que existe estoque de matéria prima para {string} criado ontem com {int} unidades")
    fun que_existe_estoque_mp_ontem(saborNome: String, qtd: Int) {
        val sabor = saborRepository.findAll().first { it.nome == saborNome }
        // Create manually to set date
        val mp = br.com.pinguimice.admin.entity.MateriaPrima(
            nome = "MP Ontem",
            sabor = sabor,
            tipoEntrada = "PACOTE",
            quantidadeEntrada = BigDecimal.ONE,
            precoEntrada = BigDecimal.TEN,
            totalUnidades = qtd,
            precoPorUnidade = BigDecimal.ONE,
            estoqueUnidades = qtd
        )
        mp.dataCriacao = LocalDateTime.now().minusDays(1)
        materiaPrimaRepository.save(mp)
    }

    @Given("que existe estoque de matéria prima para {string} criado hoje com {int} unidades")
    fun que_existe_estoque_mp_hoje(saborNome: String, qtd: Int) {
        val sabor = saborRepository.findAll().first { it.nome == saborNome }
        val mp = br.com.pinguimice.admin.entity.MateriaPrima(
            nome = "MP Hoje",
            sabor = sabor,
            tipoEntrada = "PACOTE",
            quantidadeEntrada = BigDecimal.ONE,
            precoEntrada = BigDecimal.TEN,
            totalUnidades = qtd,
            precoPorUnidade = BigDecimal.ONE,
            estoqueUnidades = qtd
        )
        materiaPrimaRepository.save(mp)
    }

    @Given("que existe estoque de embalagem para {string} com {int} unidades")
    fun que_existe_estoque_emb(saborNome: String, qtd: Int) {
        val sabor = saborRepository.findAll().first { it.nome == saborNome }
        val emb = br.com.pinguimice.admin.entity.Embalagem(
            nome = "Emb Teste",
            sabor = sabor,
            quantidadeKg = BigDecimal.ONE,
            precoKg = BigDecimal.TEN,
            totalUnidades = qtd,
            precoPorUnidade = BigDecimal.ONE,
            estoqueUnidades = qtd
        )
        embalagemRepository.save(emb)
    }

    @Given("que existe estoque de {string} suficiente")
    fun que_existe_estoque_outros(nome: String) {
        val outros = br.com.pinguimice.admin.entity.Outros(
            nome = nome,
            quantidadeEntrada = 100,
            precoEntrada = BigDecimal.TEN,
            unidadesPorItem = 50,
            totalUnidades = 5000,
            precoPorUnidade = BigDecimal.ONE,
            estoqueUnidades = 5000
        )
        outrosRepository.save(outros)
    }

    @When("eu registro uma produção de {int} gelinhos de {string} com dedução de estoque")
    fun eu_registro_producao(qtd: Int, saborNome: String) {
        val sabor = saborRepository.findAll().first { it.nome == saborNome }
        producaoService.registrarProducao(
            ProducaoRequest(
                saborId = sabor.id,
                quantidadeProduzida = qtd,
                deduzirEstoque = true
            )
        )
    }

    @Then("o estoque de matéria prima de ontem deve ser {int}")
    fun estoque_mp_ontem_deve_ser(qtd: Int) {
        val mps = materiaPrimaRepository.findAll().sortedBy { it.dataCriacao }
        assertEquals(qtd, mps[0].estoqueUnidades)
    }

    @Then("o estoque de matéria prima de hoje deve ser {int}")
    fun estoque_mp_hoje_deve_ser(qtd: Int) {
        val mps = materiaPrimaRepository.findAll().sortedBy { it.dataCriacao }
        assertEquals(qtd, mps[1].estoqueUnidades)
    }

    @Then("o estoque de gelinho de {string} deve aumentar em {int}")
    fun estoque_gelinho_deve_aumentar(saborNome: String, qtd: Int) {
        val sabor = saborRepository.findAll().first { it.nome == saborNome }
        val estoque = estoqueGelinhoRepository.findBySaborId(sabor.id)
        assertEquals(qtd, estoque?.quantidade)
    }

    @When("eu tento registrar uma produção de {int} gelinhos de {string} com dedução de estoque")
    fun eu_tento_registrar_producao(qtd: Int, saborNome: String) {
        val sabor = saborRepository.findAll().first { it.nome == saborNome }
        try {
            producaoService.registrarProducao(
                ProducaoRequest(
                    saborId = sabor.id,
                    quantidadeProduzida = qtd,
                    deduzirEstoque = true
                )
            )
        } catch (e: Exception) {
            lastException = e
        }
    }

    @Then("o sistema deve retornar um erro de estoque insuficiente")
    fun sistema_retorna_erro_estoque() {
        assertNotNull(lastException)
        // Check message content roughly
        assertTrue(lastException!!.message!!.contains("insuficiente", ignoreCase = true))
    }

    private var lastProducaoId: UUID? = null

    @When("eu excluo a última produção registrada")
    fun eu_excluo_ultima_producao() {
        val ultimaProducao = producaoRepository.findAllByOrderByDataProducaoDesc().firstOrNull()
            ?: fail("Nenhuma produção encontrada")
        lastProducaoId = ultimaProducao.id
        producaoService.excluirProducao(ultimaProducao.id)
    }

    @Then("o estoque de gelinho de {string} deve ser {int}")
    fun estoque_gelinho_deve_ser(saborNome: String, qtd: Int) {
        val sabor = saborRepository.findAll().first { it.nome == saborNome }
        val estoque = estoqueGelinhoRepository.findBySaborId(sabor.id)
        assertEquals(qtd, estoque?.quantidade ?: 0)
    }

    // Despesa
    @When("eu crio uma despesa {string} com valor {double}")
    fun eu_crio_despesa(descricao: String, valor: Double) {
        lastDespesaResponse = despesaService.criarDespesa(
            DespesaRequest(descricao = descricao, valor = BigDecimal.valueOf(valor))
        )
    }

    @Then("a despesa {string} deve ser listada com sucesso")
    fun despesa_deve_ser_listada(descricao: String) {
        val despesas = despesaService.listarDespesas(null, null)
        assertTrue(despesas.any { it.descricao == descricao })
    }

    @When("eu crio uma despesa {string} com valor {double} e anexo {string}")
    fun eu_crio_despesa_com_anexo(descricao: String, valor: Double, anexo: String) {
        // TestStorageService will handle the upload
        lastDespesaResponse = despesaService.criarDespesa(
            DespesaRequest(descricao = descricao, valor = BigDecimal.valueOf(valor)),
            arquivo = ByteArray(10),
            nomeArquivo = anexo,
            contentType = "application/pdf"
        )
    }

    @Then("a despesa {string} deve ter um link de anexo")
    fun despesa_deve_ter_anexo(descricao: String) {
        assertNotNull(lastDespesaResponse?.anexoUrl)
        assertTrue(lastDespesaResponse!!.anexoUrl!!.startsWith("file://"))
    }
}
