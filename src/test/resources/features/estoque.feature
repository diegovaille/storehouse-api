Feature: Gestão de Estoque e Cálculos

  Background:
    Given que eu sou um usuário autenticado
    And que existe um sabor "Morango"

  Scenario: Adicionar Estoque de Matéria Prima (Caixa)
    When eu adiciono estoque de matéria prima "Aptil Morango" para o sabor "Morango" do tipo "CAIXA" com quantidade 1 e preço 54.00
    Then o sistema deve calcular 528 unidades totais
    And o preço por unidade deve ser aproximadamente 0.10227

  Scenario: Adicionar Estoque de Embalagem (KG)
    When eu adiciono estoque de embalagem "Saquinho Coco" para o sabor "Morango" com 1 kg e preço 20.00
    Then o sistema deve calcular 700 unidades totais
    And o preço por unidade deve ser aproximadamente 0.0286

  Scenario: Adicionar Estoque de Outros (Plástico)
    When eu adiciono estoque de outros "Plástico" com quantidade 100, preço 50.00 e unidades por item 50
    Then o sistema deve calcular 5000 unidades totais
