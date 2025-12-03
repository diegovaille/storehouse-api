package br.com.storehouse.cucumber

import br.com.pinguimice.admin.model.DespesaRequest
import br.com.pinguimice.admin.model.DespesaResponse
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import java.math.BigDecimal

class DespesaSteps : BaseSteps() {

    private var lastDespesaResponse: DespesaResponse? = null

    @When("eu crio uma despesa {string} com valor {double}")
    fun euCrioDespesa(descricao: String, valor: Double) {
        lastDespesaResponse = despesaService.criarDespesa(
            DespesaRequest(descricao = descricao, valor = BigDecimal.valueOf(valor))
        )
    }

    @Then("a despesa {string} deve ser listada com sucesso")
    fun despesaDeveSerListada(descricao: String) {
        val despesas = despesaService.listarDespesas(null, null)
        assertTrue(despesas.any { it.descricao == descricao })
    }

    @When("eu crio uma despesa {string} com valor {double} e anexo {string}")
    fun euCrioDespesaComAnexo(descricao: String, valor: Double, anexo: String) {
        // TestStorageService will handle the upload
        lastDespesaResponse = despesaService.criarDespesa(
            DespesaRequest(descricao = descricao, valor = BigDecimal.valueOf(valor)),
            arquivo = ByteArray(10),
            nomeArquivo = anexo,
            contentType = "application/pdf"
        )
    }

    @Then("a despesa {string} deve ter um link de anexo")
    fun despesaDeveTerAnexo(descricao: String) {
        assertNotNull(lastDespesaResponse?.anexoUrl)
        assertTrue(lastDespesaResponse!!.anexoUrl!!.startsWith("file://"))
    }
}
