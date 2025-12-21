package br.com.storehouse.cucumber

import br.com.pinguimice.admin.controller.PinguimVendaController
import br.com.pinguimice.admin.model.PinguimVendaItemRequest
import br.com.pinguimice.admin.model.PinguimVendaRequest
import br.com.pinguimice.admin.model.PinguimVendaResponse
import br.com.storehouse.data.SharedTestData
import br.com.storehouse.data.model.UsuarioAutenticado
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.math.BigDecimal

class PinguimVendaSteps(
    private val sharedTestData: SharedTestData
) : BaseSteps() {

    @Autowired
    private lateinit var pinguimVendaController: PinguimVendaController

    private var lastVendaResponse: ResponseEntity<PinguimVendaResponse>? = null
    private var lastError: Exception? = null

    @When("eu registro uma venda para o cliente {string} com os seguintes itens:")
    fun euRegistroUmaVendaParaARegiaoComOsSeguintesItens(nomeCliente: String, dataTable: DataTable) {
        val cliente = clienteRepository.findAll().first { it.nome == nomeCliente }
        val itens = dataTable.asMaps().map { row ->
            val sabor = saborRepository.findAll().first { it.nome == row["Sabor"] }
            PinguimVendaItemRequest(
                saborId = sabor.id,
                quantidade = row["Quantidade"]!!.toInt()
            )
        }

        val request = PinguimVendaRequest(
            clienteId = cliente.id,
            itens = itens,
            total = BigDecimal("100.00"),
            totalPago = BigDecimal("100.00")
        )

        try {
            lastVendaResponse = pinguimVendaController.registrarVenda(request, sharedTestData.usuarioAutenticado!!)
        } catch (e: Exception) {
            lastError = e
        }
    }

    @Then("a venda deve ser registrada com sucesso")
    fun aVendaDeveSerRegistradaComSucesso() {
        assertNotNull(lastVendaResponse)
        assertEquals(HttpStatus.CREATED, lastVendaResponse!!.statusCode)
    }

    @Then("o estoque do sabor {string} deve ser {int}")
    fun oEstoqueDoSaborDeveSer(nomeSabor: String, quantidadeEsperada: Int) {
        val sabor = saborRepository.findAll().first { it.nome == nomeSabor }
        val estoque = estoqueGelinhoRepository.findBySaborId(sabor.id)!!
        assertEquals(quantidadeEsperada, estoque.quantidade)
    }

    @When("eu tento registrar uma venda para o cliente {string} com os seguintes itens:")
    fun euTentoRegistrarUmaVendaParaOClienteComOsSeguintesItens(nomeCliente: String, dataTable: DataTable) {
        euRegistroUmaVendaParaARegiaoComOsSeguintesItens(nomeCliente, dataTable)
    }

    @Then("a venda não deve ser registrada")
    fun aVendaNaoDeveSerRegistrada() {
        assertNotNull(lastError)
    }

    @Then("deve retornar um erro de estoque insuficiente")
    fun deveRetornarUmErroDeEstoqueInsuficiente() {
        assertNotNull(lastError)
        // Check if the exception message contains "Estoque insuficiente"
        // Depending on how exceptions are wrapped, we might need to check cause or message
        // For now, assuming the exception thrown by service bubbles up
        assert(lastError!!.message?.contains("Estoque insuficiente") == true)
    }

    @Given("que foi registrada uma venda para o cliente {string} com {int} gelinhos de {string}")
    fun queFoiRegistradaUmaVendaParaOClienteComGelinhosDe(nomeCliente: String, quantidade: Int, nomeSabor: String) {
        val cliente = clienteRepository.findAll().first { it.nome == nomeCliente }
        val sabor = saborRepository.findAll().first { it.nome == nomeSabor }

        val request = PinguimVendaRequest(
            clienteId = cliente.id,
            itens = listOf(
                PinguimVendaItemRequest(
                    saborId = sabor.id,
                    quantidade = quantidade
                )
            ),
            total = BigDecimal("10.00"),
            totalPago = BigDecimal("10.00")
        )

        lastVendaResponse = pinguimVendaController.registrarVenda(request, sharedTestData.usuarioAutenticado!!)
    }

    @When("eu cancelo a venda")
    fun euCanceloAVenda() {
        val id = lastVendaResponse!!.body!!.id
        pinguimVendaController.cancelarVenda(id, sharedTestData.usuarioAutenticado!!)
    }

    @Then("a venda deve ser cancelada com sucesso")
    fun aVendaDeveSerCanceladaComSucesso() {
        // If no exception was thrown in the previous step, it's a success
    }
}
