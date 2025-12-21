package br.com.storehouse.cucumber

import br.com.pinguimice.admin.entity.TipoEntrada
import br.com.pinguimice.admin.model.*
import br.com.storehouse.data.SharedTestData
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import java.math.BigDecimal

class EstoqueSteps(private val sharedTestData: SharedTestData) : BaseSteps() {

    private var lastMateriaPrimaResponse: MateriaPrimaResponse? = null
    private var lastEmbalagemResponse: EmbalagemResponse? = null
    private var lastOutrosResponse: OutrosResponse? = null
    private var lastSaborResponse: SaborResponse? = null

    @Given("que existe um sabor {string}")
    fun queExisteUmSabor(nome: String) {
        lastSaborResponse = saborService.criarSabor(SaborRequest(nome = nome, corHex = "#FFFFFF"), sharedTestData.filial!!.id)
    }

    @Given("que existe um sabor {string} que usa açúcar")
    fun queExisteUmSaborQueUsaAcucar(nome: String) {
        lastSaborResponse = saborService.criarSabor(SaborRequest(nome = nome, corHex = "#FFFFFF", usaAcucar = true), sharedTestData.filial!!.id)
    }

    @When("eu adiciono estoque de matéria prima {string} para o sabor {string} do tipo {string} com quantidade {double} e preço {double}")
    fun euAdicionoEstoqueMateriaPrima(nomeMp: String, nomeSabor: String, tipo: String, qtd: Double, preco: Double) {
        val sabor = saborRepository.findAll().first { it.nome == nomeSabor }
        lastMateriaPrimaResponse = estoqueService.criarMateriaPrima(
            MateriaPrimaRequest(
                nome = nomeMp,
                saborId = sabor.id,
                tipoEntrada = TipoEntrada.valueOf(tipo),
                quantidadeEntrada = BigDecimal.valueOf(qtd),
                precoEntrada = BigDecimal.valueOf(preco)
            ),
            sharedTestData.filial!!.id
        )
    }

    @Then("o sistema deve calcular {int} unidades totais")
    fun oSistemaDeveCalcularUnidades(unidades: Int) {
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
    fun oPrecoPorUnidadeDeveSer(preco: Double) {
        val actual = lastMateriaPrimaResponse?.precoPorUnidade 
            ?: lastEmbalagemResponse?.precoPorUnidade 
            ?: lastOutrosResponse?.precoPorUnidade
            ?: fail("Nenhuma resposta encontrada")
            
        assertEquals(preco, actual.toDouble(), 0.0001)
    }

    @When("eu adiciono estoque de embalagem {string} para o sabor {string} com {double} kg e preço {double}")
    fun euAdicionoEstoqueEmbalagem(nomeEmb: String, nomeSabor: String, qtd: Double, preco: Double) {
        val sabor = saborRepository.findAll().first { it.nome == nomeSabor }
        lastEmbalagemResponse = estoqueService.criarEmbalagem(
            EmbalagemRequest(
                nome = nomeEmb,
                saborId = sabor.id,
                quantidadeKg = BigDecimal.valueOf(qtd),
                precoKg = BigDecimal.valueOf(preco)
            ),
            sharedTestData.filial!!.id
        )
    }

    @When("eu adiciono estoque de outros {string} com quantidade {int}, preço {double} e unidades por item {int}")
    fun euAdicionoEstoqueOutros(nome: String, qtd: Int, preco: Double, unidadesPorItem: Int) {
        lastOutrosResponse = estoqueService.criarOutros(
            OutrosRequest(
                nome = nome,
                quantidadeEntrada = qtd,
                precoEntrada = BigDecimal.valueOf(preco),
                unidadesPorItem = unidadesPorItem
            ),
            sharedTestData.filial!!.id
        )
    }
}
