package br.com.storehouse.api.handler

import br.com.storehouse.api.security.JwtUtils
import br.com.storehouse.service.UsuarioService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.net.URLEncoder

/**
 * Handles successful Google OAuth2 authentication.
 *
 * After authentication, the user is redirected to the correct frontend (production or preview).
 *
 * HOW IT WORKS:
 * - Before initiating the Google login, the frontend sets a short-lived cookie `oauth_origin`
 *   with its own `window.location.origin` (e.g. https://preview.primeira.app.br).
 * - Spring Security manages the `state` param internally for CSRF — we do NOT touch it.
 * - After authentication, this handler reads the `oauth_origin` cookie and validates it
 *   against [ALLOWED_ORIGINS] before redirecting.
 * - If the cookie is absent or invalid, the fallback [frontendBaseUrl] is used (production).
 *
 * SECURITY: Only origins in [ALLOWED_ORIGINS] are accepted — prevents open-redirect attacks.
 */
@Component
class OAuth2SuccessHandler(
    private val jwtUtils: JwtUtils,
    private val usuarioService: UsuarioService
) : AuthenticationSuccessHandler {

    @Value("\${app.frontend-base-url:}")
    private lateinit var frontendBaseUrl: String

    private val logger = LoggerFactory.getLogger(OAuth2SuccessHandler::class.java)

    /**
     * Whitelist of origins allowed to receive the OAuth redirect.
     * Add new environments here (e.g. staging) as needed.
     */
    private val ALLOWED_ORIGINS = setOf(
        "https://primeira.app.br",
        "https://preview.primeira.app.br"
    )

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oauthToken = authentication as OAuth2AuthenticationToken
        val attributes = oauthToken.principal.attributes

        val email = attributes["email"] as? String
            ?: throw RuntimeException("Email não encontrado na autenticação OAuth2")

        val name = attributes["name"] as? String
        val picture = attributes["picture"] as? String

        usuarioService.buscarPorEmail(email)
            ?: throw RuntimeException("Usuário não autorizado: $email")

        val tokenTemporario = jwtUtils.generateTempToken(email)

        // 1. Read the `oauth_origin` cookie set by the frontend before the OAuth redirect.
        val cookieOrigin = request.cookies
            ?.firstOrNull { it.name == "oauth_origin" }
            ?.value
            ?.takeIf { it in ALLOWED_ORIGINS }

        // 2. Determine target frontend base URL.
        val redirectBase = when {
            cookieOrigin != null -> {
                logger.info("Usando oauth_origin cookie: $cookieOrigin")
                cookieOrigin
            }
            frontendBaseUrl.isNotBlank() -> frontendBaseUrl
            else -> getBaseUrl(request)
        }

        val redirectUrl = buildString {
            append("$redirectBase/organizacao.html")
            append("#tempToken=$tokenTemporario")
            if (name != null) append("&name=${URLEncoder.encode(name, "UTF-8")}")
            if (picture != null) append("&picture=${URLEncoder.encode(picture, "UTF-8")}")
        }

        logger.info("Usuário autenticado: $email → redirecionando para: $redirectUrl")
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