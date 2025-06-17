package br.com.storehouse.data.model

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.*

data class UsuarioAutenticado(
    val email: String,
    val perfil: String,
    val organizacaoId: UUID,
    val filialId: UUID,
    private val authoritiesList: Collection<GrantedAuthority> = listOf()
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> = authoritiesList
    override fun getPassword(): String? = null
    override fun getUsername(): String = email
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true
}
