Feature: Gestão de Sabores

  Background:
    Given que eu sou um usuário autenticado

  Scenario: Criar um novo sabor
    When eu crio um sabor com nome "Morango" e cor "#FF0000"
    Then o sabor "Morango" deve ser listado com sucesso

  Scenario: Listar sabores ativos
    Given que existe um sabor "Limão" ativo
    And que existe um sabor "Uva" inativo
    When eu listo os sabores ativos
    Then eu devo ver o sabor "Limão"
    And eu não devo ver o sabor "Uva"
