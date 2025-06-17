package br.com.storehouse.api.controller

import br.com.storehouse.data.model.TipoProdutoDto
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/produtos/tipos")
class TipoProdutoController(
    private val tipoProdutoService: br.com.storehouse.service.TipoProdutoService,
    private val objectMapper: ObjectMapper
) {

    @GetMapping
    fun listarTipos(): List<TipoProdutoDto> = tipoProdutoService.listarTodos().map {
        val camposMap: Map<String, String> = objectMapper.readValue(it.campos, object : TypeReference<Map<String, String>>() {})
        TipoProdutoDto(it.id, it.nome, camposMap)
    }
}