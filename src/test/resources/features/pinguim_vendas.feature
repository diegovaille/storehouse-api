Feature: Vendas Pinguim Ice Admin

  Background:
    Given que existe um usuário "admin" com email "admin@pinguimice.com.br" e senha "123456"
    And que existe uma organização "Pinguim Ice" com CNPJ "60774613000108"
    And que o usuário "admin" pertence à organização "Pinguim Ice" com perfil "ADMIN"
    And que existe uma filial "Matriz" vinculada à organização "Pinguim Ice"
    And que existe um sabor "Morango" com cor "#FF0000"
    And que existe estoque de gelinho para o sabor "Morango" com quantidade 100
    And que existe uma região de venda "Centro"
    And que existe um cliente "Loja A" na região "Centro"

  Scenario: Registrar uma venda com sucesso
    Given que eu estou autenticado como "admin@pinguimice.com.br"
    When eu registro uma venda para o cliente "Loja A" com os seguintes itens:
      | Sabor   | Quantidade |
      | Morango | 10         |
    Then a venda deve ser registrada com sucesso
    And o estoque do sabor "Morango" deve ser 90

  Scenario: Tentar registrar venda sem estoque suficiente
    Given que eu estou autenticado como "admin@pinguimice.com.br"
    When eu tento registrar uma venda para o cliente "Loja A" com os seguintes itens:
      | Sabor   | Quantidade |
      | Morango | 101        |
    Then a venda não deve ser registrada
    And deve retornar um erro de estoque insuficiente

  Scenario: Cancelar uma venda e restaurar estoque
    Given que eu estou autenticado como "admin@pinguimice.com.br"
    And que foi registrada uma venda para o cliente "Loja A" com 10 gelinhos de "Morango"
    When eu cancelo a venda
    Then a venda deve ser cancelada com sucesso
    And o estoque do sabor "Morango" deve ser 100
