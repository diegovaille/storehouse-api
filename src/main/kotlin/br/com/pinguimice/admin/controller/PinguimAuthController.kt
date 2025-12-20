package br.com.pinguimice.admin.controller

import br.com.storehouse.api.security.JwtUtils
import br.com.storehouse.service.UsuarioService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/pinguimice-admin/auth")
class PinguimAuthController(
    private val jwtUtils: JwtUtils,
    private val usuarioService: UsuarioService
) {

    companion object {
        private const val PINGUIM_ICE_CNPJ = "60774613000108"
    }

    @PostMapping("/login")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    fun login(@RequestBody request: PinguimLoginRequest): ResponseEntity<Any> {
        // 1. Validate User
        val usuario = usuarioService.buscarPorUsername(request.username)
            ?: usuarioService.buscarPorEmail(request.username) ?:
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Usuário não encontrado"))

        // 2. Validate Password
        if (!usuarioService.senhaValida(usuario, request.password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Senha inválida"))
        }

        // 3. Get Organizations
        val organizacoes = usuarioService.buscarOrganizacoesDoUsuario(usuario.email)
        if (organizacoes.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "Usuário não possui vínculo com nenhuma organização"))
        }

        // 4. Validate Pinguim Ice CNPJ
        val organizacaoUsuario = organizacoes.firstOrNull {
            it.organizacao.cnpj == PINGUIM_ICE_CNPJ
        } ?: return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(mapOf("error" to "Acesso permitido apenas para usuários do Pinguim Ice"))

        val organizacao = organizacaoUsuario.organizacao

        if (organizacao.filiais.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "Organização não possui filiais cadastradas"))
        }

        // 5. Auto-select first branch
        val filial = organizacao.filiais.first()

        // 6. Generate Token
        val token = jwtUtils.generateJwt(
            email = usuario.email,
            perfil = organizacaoUsuario.perfil.tipo,
            organizacaoId = organizacao.id,
            filialId = filial.id
        )

        // 7. Return Simplified Response
        return ResponseEntity.ok(
            PinguimLoginResponse(
                token = token,
                email = usuario.email,
                organizacaoId = organizacao.id,
                filialId = filial.id,
                perfil = organizacaoUsuario.perfil.tipo
            )
        )
    }
}

data class PinguimLoginRequest(
    val username: String,
    val password: String
)

data class PinguimLoginResponse(
    val token: String,
    val email: String,
    val organizacaoId: UUID,
    val filialId: UUID,
    val perfil: String
)
