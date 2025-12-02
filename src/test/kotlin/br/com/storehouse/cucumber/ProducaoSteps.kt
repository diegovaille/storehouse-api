package br.com.storehouse.cucumber

import br.com.pinguimice.admin.model.ProducaoRequest
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class ProducaoSteps : BaseSteps() {

    private var lastProducaoId: UUID? = null

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
}
