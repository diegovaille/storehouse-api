package br.com.storehouse.cucumber

import br.com.pinguimice.admin.entity.EstoqueGelinho
import br.com.pinguimice.admin.entity.RegiaoVenda
import br.com.pinguimice.admin.entity.Sabor
import br.com.storehouse.data.SharedTestData
import br.com.storehouse.data.entities.*
import br.com.storehouse.data.model.UsuarioAutenticado
import io.cucumber.java.en.Given
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.LocalDateTime
import java.util.UUID

class PinguimBackgroundSteps(
    private val sharedTestData: SharedTestData
) : BaseSteps() {

    private val passwordEncoder = BCryptPasswordEncoder()

    @Given("que existe um usuário {string} com email {string} e senha {string}")
    fun queExisteUmUsuarioComEmailESenha(username: String, email: String, senha: String) {
        val usuario = Usuario(
            username = username,
            email = email,
            password = passwordEncoder.encode(senha)
        )
        usuarioRepository.save(usuario)
    }

    @Given("que existe uma organização {string} com CNPJ {string}")
    fun queExisteUmaOrganizacaoComCnpj(nome: String, cnpj: String) {
        if (organizacaoRepository.findByCnpj(cnpj) == null) {
            val organizacao = Organizacao(
                nome = nome,
                cnpj = cnpj,
                logoUrl = "http://example.com/logo.png"
            )
            organizacaoRepository.save(organizacao)
        }
    }

    @Given("que o usuário {string} pertence à organização {string} com perfil {string}")
    fun queOUsuarioPertenceAOrganizacaoComPerfil(username: String, orgNome: String, perfilNome: String) {
        val usuario = usuarioRepository.findByUsername(username)!!
        val organizacao = organizacaoRepository.findAll().first { it.nome == orgNome }
        
        var perfil = perfilRepository.findByTipo(perfilNome)
        if (perfil == null) {
            perfil = Perfil(tipo = perfilNome)
            perfilRepository.save(perfil)
        }

        val vinculo = OrganizacaoUsuario(
            usuario = usuario,
            organizacao = organizacao,
            perfil = perfil
        )
        organizacaoUsuarioRepository.save(vinculo)
    }

    @Given("que existe uma filial {string} vinculada à organização {string}")
    fun queExisteUmaFilialVinculadaAOrganizacao(filialNome: String, orgNome: String) {
        val organizacao = organizacaoRepository.findAll().first { it.nome == orgNome }
        val filial = Filial(
            nome = filialNome,
            organizacao = organizacao
        )
        filialRepository.save(filial)
    }

    @Given("que existe um sabor {string} com cor {string}")
    fun queExisteUmSaborComCor(nome: String, cor: String) {
        val sabor = Sabor(
            nome = nome,
            corHex = cor
        )
        saborRepository.save(sabor)
    }

    @Given("que existe estoque de gelinho para o sabor {string} com quantidade {int}")
    fun queExisteEstoqueDeGelinhoParaOSaborComQuantidade(nomeSabor: String, quantidade: Int) {
        val sabor = saborRepository.findAll().first { it.nome == nomeSabor }
        val estoque = EstoqueGelinho(
            sabor = sabor,
            quantidade = quantidade,
            ultimaAtualizacao = LocalDateTime.now()
        )
        estoqueGelinhoRepository.save(estoque)
    }

    @Given("que existe uma região de venda {string}")
    fun queExisteUmaRegiaoDeVenda(nome: String) {
        val regiao = RegiaoVenda(
            nome = nome
        )
        regiaoVendaRepository.save(regiao)
    }

    @Given("que eu estou autenticado como {string}")
    fun queEstouAutenticadoComoUsuario(email: String) {
        val usuario = usuarioRepository.findByEmail(email)!!

        val userDetails = UsuarioAutenticado(
            email = usuario.email,
            perfil = "ADMIN",
            organizacaoId = UUID.randomUUID(),
            filialId = UUID.randomUUID()
        )
        val auth = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
        SecurityContextHolder.getContext().authentication = auth

        sharedTestData.usuarioAutenticado = userDetails
    }
}
