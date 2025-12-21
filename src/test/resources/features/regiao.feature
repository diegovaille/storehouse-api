Feature: Gestão de Regiões de Venda

  Background:
    Given que existe um usuário "admin" com email "admin@pinguimice.com.br" e senha "123456"
    And que existe uma organização "Pinguim Ice" com CNPJ "60774613000108"
    And que o usuário "admin" pertence à organização "Pinguim Ice" com perfil "ADMIN"
    And que existe uma filial "Matriz" vinculada à organização "Pinguim Ice"
    And que eu estou autenticado como "admin@pinguimice.com.br"

  Scenario: Criar uma nova região
    When eu crio uma região com nome "Zona Norte"
    Then a região "Zona Norte" deve ser listada com sucesso

  Scenario: Atualizar região
    Given que existe uma região "Zona Sul"
    When eu atualizo o nome da região "Zona Sul" para "Zona Sul Expandida"
    Then a região deve ser atualizada para "Zona Sul Expandida"
