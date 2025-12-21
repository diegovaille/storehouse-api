Feature: Gestão de Sabores

  Background:
    Given que existe um usuário "admin" com email "admin@pinguimice.com.br" e senha "123456"
    And que existe uma organização "Pinguim Ice" com CNPJ "60774613000108"
    And que o usuário "admin" pertence à organização "Pinguim Ice" com perfil "ADMIN"
    And que existe uma filial "Matriz" vinculada à organização "Pinguim Ice"
    And que eu estou autenticado como "admin@pinguimice.com.br"

  Scenario: Criar um novo sabor
    When eu crio um sabor com nome "Morango" e cor "#FF0000"
    Then o sabor "Morango" deve ser listado com sucesso

  Scenario: Listar sabores ativos
    Given que existe um sabor "Limão" ativo
    And que existe um sabor "Uva" inativo
    When eu listo os sabores ativos
    Then eu devo ver o sabor "Limão"
    And eu não devo ver o sabor "Uva"
