package br.com.storehouse.api.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtUtils(
    @Value("\${jwt.secret}")
    private val jwtSecret: String,
    @Value("\${jwt.expiration}")
    private val jwtExpirationMs: Long
) {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())

    fun generateJwt(email: String, perfil: String, organizacaoId: UUID, filialId: UUID): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpirationMs)

        return Jwts.builder()
            .setSubject(email)
            .claim("perfil", perfil)
            .claim("organizacaoId", organizacaoId.toString())
            .claim("filialId", filialId.toString())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact()
    }

    fun generateTempToken(email: String): String {
        val now = Date()
        val expiryDate = Date(now.time + 5 * 60 * 1000) // 5 minutos

        return Jwts.builder()
            .setSubject(email)
            .claim("tipo", "TEMP")
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact()
    }

    fun isTempToken(token: String): Boolean {
        val tipo = parseClaims(token)["tipo"] as? String
        return tipo == "TEMP"
    }

    fun getEmailFromJwt(token: String): String =
        parseClaims(token).subject

    fun getPerfilFromJwt(token: String): String? =
        parseClaims(token)["perfil"] as? String

    fun getOrganizacaoIdFromJwt(token: String): UUID? =
        (parseClaims(token)["organizacaoId"] as? String)?.let { UUID.fromString(it) }

    fun getFilialIdFromJwt(token: String): UUID? =
        (parseClaims(token)["filialId"] as? String)?.let { UUID.fromString(it) }

    fun validateJwt(token: String): Boolean =
        try {
            parseClaims(token)
            true
        } catch (ex: Exception) {
            false
        }

    private fun parseClaims(token: String): Claims =
        Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .body
}