package br.com.pinguimice.admin.service

import br.com.pinguimice.admin.entity.MateriaPrima
import br.com.pinguimice.admin.entity.Sabor
import br.com.pinguimice.admin.entity.TipoEntrada
import br.com.pinguimice.admin.model.MateriaPrimaRequest
import br.com.storehouse.config.TestApplication
import br.com.storehouse.config.LiquibaseTestRunner
import br.com.storehouse.config.PostgresTestContainer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTest(classes = [TestApplication::class, LiquibaseTestRunner::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(initializers = [PostgresTestContainer.Initializer::class])
@ActiveProfiles("test")
@Transactional
class EstoqueServiceTest {

    @Autowired
    private lateinit var estoqueService: EstoqueService

    @Autowired
    private lateinit var saborRepository: br.com.pinguimice.admin.repository.SaborRepository

    @Autowired
    private lateinit var materiaPrimaRepository: br.com.pinguimice.admin.repository.MateriaPrimaRepository

    private lateinit var saborTeste: Sabor

    @BeforeEach
    fun setup() {
        saborTeste = saborRepository.findAll().firstOrNull { it.nome == "Teste" } ?: saborRepository.save(
            Sabor(nome = "Teste", corHex = "#000000")
        )
    }

    @Test
    fun `criar materia prima em KG sem sabor deve usar TOTAL_UNIDADE_ACUCAR_POR_KG`() {
        val request = MateriaPrimaRequest(
            nome = "Acucar Test",
            saborId = null,
            tipoEntrada = TipoEntrada.KG,
            quantidadeEntrada = BigDecimal.ONE,
            precoEntrada = BigDecimal.TEN
        )
        val response = estoqueService.criarMateriaPrima(request)
        // TOTAL_UNIDADE_ACUCAR_POR_KG == 222
        assertEquals(222, response.totalUnidades)
    }

    @Test
    fun `reverter deducao estoque deve restaurar usando reverse FIFO`() {
        // Cria sabor específico
        val sabor = saborRepository.save(Sabor(nome = "ReverterTest", corHex = "#111111"))

        // Cria duas matérias-primas (mais antiga e mais nova)
        val mpOld = MateriaPrima(
            nome = "MP Old",
            sabor = sabor,
            tipoEntrada = "PACOTE",
            quantidadeEntrada = BigDecimal.ONE,
            precoEntrada = BigDecimal.TEN,
            totalUnidades = 100,
            precoPorUnidade = BigDecimal.ONE,
            estoqueUnidades = 0
        )
        mpOld.dataCriacao = LocalDateTime.now().minusDays(2)
        materiaPrimaRepository.save(mpOld)

        val mpNew = MateriaPrima(
            nome = "MP New",
            sabor = sabor,
            tipoEntrada = "PACOTE",
            quantidadeEntrada = BigDecimal.ONE,
            precoEntrada = BigDecimal.TEN,
            totalUnidades = 100,
            precoPorUnidade = BigDecimal.ONE,
            estoqueUnidades = 0
        )
        mpNew.dataCriacao = LocalDateTime.now()
        materiaPrimaRepository.save(mpNew)

        // Reverter dedução de 150 unidades: deve restaurar 100 no mais novo e 50 no mais antigo
        estoqueService.reverterDeducaoEstoque(sabor, 150)

        val lista = materiaPrimaRepository.findBySaborIdOrderByDataCriacaoAsc(sabor.id)
        val old = lista[0]
        val newer = lista[1]
        assertEquals(50, old.estoqueUnidades)
        assertEquals(100, newer.estoqueUnidades)
    }
}
