package br.com.pinguimice.admin.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import java.math.BigDecimal

class DespesaRequestDeserializationTest {

    private val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()

    @Test
    fun `deve desserializar valor decimal vindo como string com virgula`() {
        val json = """
            {
              "descricao": "Teste",
              "valor": "10,50"
            }
        """.trimIndent()

        val req = objectMapper.readValue(json, DespesaRequest::class.java)
        assertEquals(BigDecimal("10.50"), req.valor)
    }

    @Test
    fun `deve desserializar valor decimal vindo como string com separador de milhar`() {
        val json = """
            {
              "descricao": "Teste",
              "valor": "1.234,56"
            }
        """.trimIndent()

        val req = objectMapper.readValue(json, DespesaRequest::class.java)
        assertEquals(BigDecimal("1234.56"), req.valor)
    }
}

