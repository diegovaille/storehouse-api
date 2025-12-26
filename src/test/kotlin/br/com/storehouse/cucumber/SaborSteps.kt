package br.com.storehouse.cucumber

import br.com.pinguimice.admin.model.SaborRequest
import br.com.pinguimice.admin.model.SaborResponse
import br.com.storehouse.data.SharedTestData
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class SaborSteps(private val sharedTestData: SharedTestData) : BaseSteps() {

    private var lastSaborResponse: SaborResponse? = null

    @When("eu crio um sabor com nome {string} e cor {string}")
    fun eu_crio_um_sabor(nome: String, cor: String) {
        val request = SaborRequest(nome = nome, corHex = cor)
        lastSaborResponse = saborService.criarSabor(request, sharedTestData.filial!!.id)
    }

    @Then("o sabor {string} deve ser listado com sucesso")
    fun o_sabor_deve_ser_listado(nome: String) {
        val sabores = saborService.listarSabores()
        assertTrue(sabores.any { it.nome == nome })
    }

    @Given("que existe um sabor {string} ativo")
    fun que_existe_um_sabor_ativo(nome: String) {
        saborService.criarSabor(SaborRequest(nome = nome, corHex = "#000000"), sharedTestData.filial!!.id)
    }

    @Given("que existe um sabor {string} inativo")
    fun que_existe_um_sabor_inativo(nome: String) {
        val sabor = saborService.criarSabor(SaborRequest(nome = nome, corHex = "#000000"), sharedTestData.filial!!.id)
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
}
