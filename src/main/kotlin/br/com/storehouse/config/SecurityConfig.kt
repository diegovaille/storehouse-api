package br.com.storehouse.config

import br.com.storehouse.api.handler.OAuth2SuccessHandler
import br.com.storehouse.api.security.JwtUtils
import br.com.storehouse.api.security.filters.JwtAuthenticationFilter
import br.com.storehouse.service.UsuarioService
import org.springframework.core.env.Environment
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource


@Configuration
@EnableMethodSecurity
@Profile("!test")
class SecurityConfig(
    private val jwtUtils: JwtUtils,
    private val usuarioService: UsuarioService,
    private val oauth2SuccessHandler: OAuth2SuccessHandler,
    private val env: Environment
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { }  // 🔥 Ativa o uso do CorsConfigurationSource definido abaixo
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/oauth2/**").permitAll()
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/pinguim-admin/auth/**").permitAll()
                    .requestMatchers("/favicon.ico").permitAll()
                    .requestMatchers("/login/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/produtos/tipos").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/produtos").permitAll()
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .loginPage("/login")
                    .successHandler(oauth2SuccessHandler) // ✅ Usa o handler novo
            }
            .addFilterBefore(
                JwtAuthenticationFilter(jwtUtils),
                UsernamePasswordAuthenticationFilter::class.java
            )
            .exceptionHandling {
                it.authenticationEntryPoint { request, response, authException ->
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized")
                }
            }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationProvider(): AuthenticationProvider {
        val authProvider = DaoAuthenticationProvider()
        authProvider.setUserDetailsService(usuarioService)
        authProvider.setPasswordEncoder(passwordEncoder())
        return authProvider
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            // Produção (sempre permitidos)
            addAllowedOrigin("https://primeira.app.br")
            addAllowedOriginPattern("https://*.primeira.app.br")
            addAllowedOrigin("https://admin.pinguimice.com.br")
            addAllowedOriginPattern("https://*.pinguimice.com.br")
            
            // Dev apenas
            if (env.activeProfiles.contains("dev")) {
                addAllowedOrigin("http://localhost:*")
                addAllowedOrigin("http://127.0.0.1:*")
                addAllowedOriginPattern("https://*.ngrok-free.app")
            }
            
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

}