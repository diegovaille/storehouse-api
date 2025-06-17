package br.com.storehouse.api.controller

import br.com.storehouse.api.security.JwtUtils
import br.com.storehouse.constants.ErrorMessages
import br.com.storehouse.service.UsuarioService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val jwtUtils: JwtUtils,
    private val usuarioService: UsuarioService
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        val usuario = usuarioService.buscarPorUsername(request.username)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Usuário não encontrado"))

        if (!usuarioService.senhaValida(usuario, request.password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Senha inválida"))
        }

        val organizacoes = usuarioService.buscarOrganizacoesDoUsuario(usuario.email)
        if (organizacoes.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "Usuário não possui vínculo com nenhuma organização"))
        }

        val tempToken = jwtUtils.generateTempToken(usuario.email)

        return ResponseEntity.ok(
            mapOf(
                "email" to usuario.email,
                "tempToken" to tempToken
            )
        )
    }

    @PostMapping("/organizacoes")
    fun listarOrganizacoes(@RequestBody req: TempTokenRequest): ResponseEntity<Any> {
        logger.info("Recebendo requisição para listar organizações com token temporário: ${req.tempToken}")
        if (!jwtUtils.validateJwt(req.tempToken) || !jwtUtils.isTempToken(req.tempToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Token inválido ou expirado"))
        }

        val email = jwtUtils.getEmailFromJwt(req.tempToken)
        val organizacoesUsuario = usuarioService.buscarOrganizacoesDoUsuario(email)

        if (organizacoesUsuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to ErrorMessages.USUARIO_SEM_ORGANIZACAO))
        }

        logger.info("Usuário $email possui ${organizacoesUsuario.size} organizações vinculadas")

        val resposta = organizacoesUsuario.map {
            val filiais = it.organizacao.filiais.map { filial ->
                mapOf(
                    "filialId" to filial.id,
                    "filialNome" to filial.nome
                )
            }

            mapOf(
                "organizacaoId" to it.organizacao.id,
                "organizacaoNome" to it.organizacao.nome,
                "perfil" to it.perfil.tipo,
                "filiais" to filiais
            )
        }

        logger.info("Organizações do usuário $email: $resposta")

        return ResponseEntity.ok(
            mapOf(
                "email" to email,
                "organizacoes" to resposta
            )
        )
    }

    @PostMapping("/token")
    fun gerarToken(@RequestBody req: TokenRequest): ResponseEntity<Map<String, Any?>> {
        if (!jwtUtils.validateJwt(req.tempToken) || !jwtUtils.isTempToken(req.tempToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Token temporário inválido ou expirado"))
        }

        val email = jwtUtils.getEmailFromJwt(req.tempToken)

        val organizacaoUsuario = usuarioService.buscarPerfilDoUsuarioNaOrganizacao(email, req.organizacaoId)
            ?: return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "Usuário sem vínculo com a organização informada"))

        val finalToken = jwtUtils.generateJwt(email, organizacaoUsuario.perfil.tipo, req.organizacaoId, req.filialId)

        val organizacao = organizacaoUsuario.organizacao
        val filial = organizacaoUsuario.organizacao.filiais.find { it.id == req.filialId }
            ?: return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "Filial não encontrada para a organização informada"))

        return ResponseEntity.ok(
            mapOf(
                "token" to finalToken,
                "organizacaoId" to req.organizacaoId,
                "organizacaoNome" to organizacao.nome,
                "organizacaoLogoUrl" to organizacao.logoUrl,
                "filialId" to req.filialId,
                "filialNome" to filial.nome,
                "filialLogoUrl" to filial.logoUrl,
                "perfil" to organizacaoUsuario.perfil.tipo
            )
        )
    }
}

data class LoginRequest(val username: String, val password: String)
data class TempTokenRequest(val tempToken: String)
data class TokenRequest(val tempToken: String, val organizacaoId: UUID, val filialId: UUID)