package br.com.storehouse.cucumber

import br.com.pinguimice.admin.model.RegiaoVendaRequest
import br.com.pinguimice.admin.model.RegiaoVendaResponse
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class RegiaoSteps : BaseSteps() {

    private var lastRegiaoResponse: RegiaoVendaResponse? = null

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
}
