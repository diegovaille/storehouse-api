// src/main/kotlin/br/com/storehouse/api/controller/VendaV2Controller.kt
package br.com.storehouse.api.controller

import br.com.storehouse.data.model.UsuarioAutenticado
import br.com.storehouse.data.model.VendaRequest
import br.com.storehouse.data.model.VendaResponse
import br.com.storehouse.service.VendaService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * V2 da API de vendas. Difere do v1 apenas na regra do voucher:
 * 50% de desconto sobre UMA unidade do produto de maior preço unitário
 * (em vez de 50% sobre o total). Demais operações de venda continuam no v1.
 */
@RestController
@RequestMapping("/api/v2/vendas")
class VendaV2Controller(private val vendaService: VendaService) {

    @PostMapping
    fun realizarVenda(
        @RequestBody request: VendaRequest,
        @AuthenticationPrincipal usuario: UsuarioAutenticado,
    ): ResponseEntity<VendaResponse> {
        val venda = vendaService.registrarVenda(
            filialId = usuario.filialId,
            request = request,
            emailUsuario = usuario.email,
            voucherSomenteItemMaisCaro = true
        )
        return ResponseEntity.ok(venda)
    }
}
