Feature: Gestão de Despesas

  Background:
    Given que existe um usuário "admin" com email "admin@pinguimice.com.br" e senha "123456"
    And que existe uma organização "Pinguim Ice" com CNPJ "60774613000108"
    And que o usuário "admin" pertence à organização "Pinguim Ice" com perfil "ADMIN"
    And que existe uma filial "Matriz" vinculada à organização "Pinguim Ice"
    And que eu estou autenticado como "admin@pinguimice.com.br"

  Scenario: Criar despesa sem anexo
    When eu crio uma despesa "Conta de Luz" com valor 150.00
    Then a despesa "Conta de Luz" deve ser listada com sucesso

  Scenario: Criar despesa com anexo (Simulado)
    When eu crio uma despesa "Recibo" com valor 50.00 e anexo "recibo.pdf"
    Then a despesa "Recibo" deve ter um link de anexo
