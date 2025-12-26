package br.com.storehouse.api.security.filters

import br.com.storehouse.api.security.JwtUtils
import br.com.storehouse.data.model.UsuarioAutenticado
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtUtils: JwtUtils
) : OncePerRequestFilter() {

    private val excludedPaths = listOf(
        "/api/pinguim-admin/auth/login",
        "/api/auth/**"
    )

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.servletPath
        return excludedPaths.any { path.startsWith(it) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        logger.info("Validando JWT na requisição: ${request.requestURI}")

        val token = getTokenFromRequest(request)

        if (token != null && jwtUtils.validateJwt(token)) {
            val email = jwtUtils.getEmailFromJwt(token)
            val perfil = jwtUtils.getPerfilFromJwt(token)
            val organizacaoId = jwtUtils.getOrganizacaoIdFromJwt(token)
            val filialId = jwtUtils.getFilialIdFromJwt(token)

            if (perfil != null && organizacaoId != null && filialId != null) {
                val usuarioAutenticado = UsuarioAutenticado(
                    email = email,
                    perfil = perfil,
                    organizacaoId = organizacaoId,
                    filialId = filialId
                )

                val authentication = UsernamePasswordAuthenticationToken(
                    usuarioAutenticado,
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_${perfil.uppercase()}"))
                )
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

                SecurityContextHolder.getContext().authentication = authentication
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun getTokenFromRequest(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization")
        return if (header != null && header.startsWith("Bearer ")) {
            header.substring(7)
        } else null
    }
}