package br.com.storehouse.api.handler

import br.com.storehouse.api.security.JwtUtils
import br.com.storehouse.service.UsuarioService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.net.URLEncoder

@Component
class OAuth2SuccessHandler(
    private val jwtUtils: JwtUtils,
    private val usuarioService: UsuarioService
) : AuthenticationSuccessHandler {

    private val logger = LoggerFactory.getLogger(OAuth2SuccessHandler::class.java)

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val headers = request.headerNames.toList().associateWith { request.getHeader(it) }
        logger.info("Request headers: $headers")
        val oauthToken = authentication as OAuth2AuthenticationToken
        val attributes = oauthToken.principal.attributes

        val email = attributes["email"] as? String
            ?: throw RuntimeException("Email não encontrado na autenticação OAuth2")

        val name = attributes["name"] as? String
        val picture = attributes["picture"] as? String

        val usuario = usuarioService.buscarPorEmail(email)
            ?: throw RuntimeException("Usuário não autorizado: $email")

        val tokenTemporario = jwtUtils.generateTempToken(email)
        val baseUrl = getBaseUrl(request)

        val redirectUrl = buildString {
            append("$baseUrl/organizacao.html")
            append("#tempToken=$tokenTemporario")
            if (name != null) append("&name=${URLEncoder.encode(name, "UTF-8")}")
            if (picture != null) append("&picture=${URLEncoder.encode(picture, "UTF-8")}")
        }

        logger.info("Usuário autenticado: $email, redirecionando para: $redirectUrl")

        response.sendRedirect(redirectUrl)
    }

    private fun getBaseUrl(request: HttpServletRequest): String {
        val proto = request.getHeader("X-Forwarded-Proto") ?: request.scheme
        val host = request.getHeader("X-Forwarded-Host") ?: request.serverName
        val port = request.getHeader("X-Forwarded-Port") ?: request.serverPort.toString()

        val isStandardPort = (proto == "http" && port == "80") || (proto == "https" && port == "443")
        return if (isStandardPort) "$proto://$host" else "$proto://$host:$port"
    }
}