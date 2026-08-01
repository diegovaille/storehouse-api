package br.com.storehouse.api.security.filters

import br.com.storehouse.config.RateLimitProperties
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.ConsumptionProbe
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Rate limiting por IP para duas superfícies sensíveis: a vitrine pública anônima
 * (`/api/publico` e subrotas) e o login (`POST /api/auth/login`, superfície clássica de
 * força bruta). Toda outra rota é autenticada e de baixo volume — não passa por aqui.
 *
 * **Isto é rate limiting DE APLICAÇÃO, não proteção de DDoS.** Protege contra um único
 * cliente abusivo raspando o catálogo, martelamento acidental, e força bruta de senha. Uma
 * inundação distribuída de verdade precisa ser parada na borda (CDN/WAF — Cloudflare, AWS
 * WAF/Shield). Este filtro não substitui isso, só reduz a superfície que sobra depois dela.
 *
 * ## Armadilha 1 — chave = IP real do cliente, nunca um IP compartilhado
 *
 * A app roda atrás de proxy com `server.forward-headers-strategy: framework`
 * (`application-{dev,prod}.yml`). Essa propriedade ativa o `ForwardedHeaderFilter` do
 * Spring, registrado pelo Boot como um `FilterRegistrationBean` com
 * `Ordered.HIGHEST_PRECEDENCE` — ou seja, roda ANTES de toda a `springSecurityFilterChain`
 * (que entra com `SecurityProperties.DEFAULT_FILTER_ORDER = -100`, bem depois de
 * `HIGHEST_PRECEDENCE`). Por isso não importa em que ponto DENTRO da `SecurityFilterChain`
 * este filtro é registrado (ver `SecurityConfig`): `request.remoteAddr` já foi reescrito a
 * partir de `X-Forwarded-For` antes de qualquer filtro da cadeia de segurança rodar, este
 * incluso. Se este filtro chaveasse no IP do load balancer (ou numa constante), todo cliente
 * cairia no MESMO bucket — um DoS auto-infligido pior que não ter limite nenhum. Prova disso:
 * `RateLimitFilterSecurityTest` (isolamento por IP).
 *
 * ## Armadilha 2 — memória limitada
 *
 * Um `Map<ip, Bucket>` que nunca expira cresce sem limite — uma entrada por IP único, para
 * sempre. Os dois caches abaixo são Caffeine com `expireAfterAccess` + `maximumSize`: um IP
 * que para de bater é evacuado do cache depois do TTL (30min para a vitrine, 10min para
 * login — mais curto porque login é usado por clientes legítimos com bem menos frequência),
 * e o tamanho tem um teto duro mesmo sob um "IP novo a cada request".
 */
@Component
@Profile("!test")
class RateLimitFilter(
    rateLimitProperties: RateLimitProperties
) : OncePerRequestFilter() {

    private val publicoRegra = rateLimitProperties.publico
    private val loginRegra = rateLimitProperties.login

    private val publicoBuckets: Cache<String, Bucket> = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(30))
        .maximumSize(100_000)
        .build()

    private val loginBuckets: Cache<String, Bucket> = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(10))
        .maximumSize(100_000)
        .build()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val tipo = regraPara(request)
        if (tipo == null) {
            filterChain.doFilter(request, response)
            return
        }

        val bucket = bucketPara(tipo, request.remoteAddr)
        val probe: ConsumptionProbe = bucket.tryConsumeAndReturnRemaining(1)

        if (probe.isConsumed) {
            response.setHeader("X-RateLimit-Remaining", probe.remainingTokens.toString())
            response.setHeader("X-RateLimit-Reset", regraConfig(tipo).refillPeriodSeconds.toString())
            filterChain.doFilter(request, response)
        } else {
            val retryAfterSegundos = TimeUnit.NANOSECONDS.toSeconds(probe.nanosToWaitForRefill).coerceAtLeast(1)
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.setHeader("Retry-After", retryAfterSegundos.toString())
            response.setHeader("X-RateLimit-Remaining", "0")
            response.setHeader("X-RateLimit-Reset", retryAfterSegundos.toString())
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.characterEncoding = "UTF-8"
            response.writer.write(
                """{"error":"Muitas requisições. Tente novamente em $retryAfterSegundos segundos."}"""
            )
        }
    }

    private fun regraPara(request: HttpServletRequest): TipoDeRegra? {
        if (request.method == "OPTIONS") return null
        val path = request.requestURI
        return when {
            path.startsWith("/api/publico/") -> TipoDeRegra.PUBLICO
            request.method == "POST" && path == "/api/auth/login" -> TipoDeRegra.LOGIN
            else -> null
        }
    }

    private fun bucketPara(tipo: TipoDeRegra, ip: String): Bucket {
        val chave = "${tipo.prefixo}:$ip"
        val cache = if (tipo == TipoDeRegra.PUBLICO) publicoBuckets else loginBuckets
        return cache.get(chave) { novoBucket(tipo) }
    }

    private fun novoBucket(tipo: TipoDeRegra): Bucket {
        val regra = regraConfig(tipo)
        val bandwidth = Bandwidth.builder()
            .capacity(regra.capacity)
            .refillGreedy(regra.capacity, Duration.ofSeconds(regra.refillPeriodSeconds))
            .build()
        return Bucket.builder().addLimit(bandwidth).build()
    }

    private fun regraConfig(tipo: TipoDeRegra): RateLimitProperties.Regra =
        if (tipo == TipoDeRegra.PUBLICO) publicoRegra else loginRegra

    private enum class TipoDeRegra(val prefixo: String) {
        PUBLICO("publico"),
        LOGIN("login")
    }
}
