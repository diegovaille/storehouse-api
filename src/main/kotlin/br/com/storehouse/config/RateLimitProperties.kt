package br.com.storehouse.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Limites de rate limiting por IP aplicados por `RateLimitFilter`. Configurável via
 * `application.yml` (bloco `rate-limit:`) para que ajuste de cota não exija deploy de código —
 * ver bloco de defaults comentado em `application.yml`.
 *
 * Os defaults abaixo são os valores de produção decididos: vitrine pública anônima
 * (`/api/publico` e subrotas, 60 requisições/minuto) e login (`POST /api/auth/login`,
 * superfície de força bruta, 10 requisições/5 minutos).
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
class RateLimitProperties {
    var publico: Regra = Regra(capacity = 60, refillPeriodSeconds = 60)
    var login: Regra = Regra(capacity = 5, refillPeriodSeconds = 60)

    class Regra(
        var capacity: Long = 0,
        var refillPeriodSeconds: Long = 0
    )
}
