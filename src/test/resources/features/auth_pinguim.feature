Feature: Autenticação Pinguim Ice

  Scenario: Login bem-sucedido
    Given que existe um usuário "admin@pinguimice.com.br" com senha "senha123"
    And que esse usuário pertence ao Pinguim Ice na filial "Matriz"
    When eu tento fazer login no Pinguim Ice com usuário "admin@pinguimice.com.br" e senha "senha123"
    Then o sistema deve retornar um token de acesso válido
    And o sistema deve retornar o email "admin@pinguimice.com.br"
    And o sistema deve retornar o perfil "ADMIN"

  Scenario: Acesso negado para usuários de outras organizações
    Given que existe um usuário "outro@empresa.com.br" com senha "senha123"
    And que esse usuário pertence à organização "Outra Empresa" com CNPJ "12345678000199"
    When eu tento fazer login no Pinguim Ice com usuário "outro@empresa.com.br" e senha "senha123"
    Then o sistema deve negar acesso para usuários fora do Pinguim Ice
    And o sistema deve retornar status 403