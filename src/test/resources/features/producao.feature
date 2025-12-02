Feature: Produção e Dedução de Estoque FIFO

  Background:
    Given que eu sou um usuário autenticado
    And que existe um sabor "Morango"
    And que existe estoque de matéria prima para "Morango" criado ontem com 100 unidades
    And que existe estoque de matéria prima para "Morango" criado hoje com 100 unidades
    And que existe estoque de embalagem para "Morango" com 500 unidades
    And que existe estoque de "Plástico" suficiente

  Scenario: Produção com Dedução FIFO
    When eu registro uma produção de 150 gelinhos de "Morango" com dedução de estoque
    Then o estoque de matéria prima de ontem deve ser 0
    And o estoque de matéria prima de hoje deve ser 50
    And o estoque de gelinho de "Morango" deve aumentar em 150

  Scenario: Produção sem Estoque Suficiente
    Given que existe estoque de matéria prima para "Morango" criado hoje com 50 unidades
    When eu tento registrar uma produção de 1500 gelinhos de "Morango" com dedução de estoque
    Then o sistema deve retornar um erro de estoque insuficiente

  Scenario: Excluir Produção e Reverter Estoque
    When eu registro uma produção de 150 gelinhos de "Morango" com dedução de estoque
    Then o estoque de matéria prima de ontem deve ser 0
    And o estoque de matéria prima de hoje deve ser 50
    And o estoque de gelinho de "Morango" deve aumentar em 150
    When eu excluo a última produção registrada
    Then o estoque de matéria prima de hoje deve ser 100
    And o estoque de matéria prima de ontem deve ser 100
    And o estoque de gelinho de "Morango" deve ser 0
