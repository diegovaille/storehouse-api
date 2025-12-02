Feature: Gestão de Regiões de Venda

  Background:
    Given que eu sou um usuário autenticado

  Scenario: Criar uma nova região
    When eu crio uma região com nome "Zona Norte"
    Then a região "Zona Norte" deve ser listada com sucesso

  Scenario: Atualizar região
    Given que existe uma região "Zona Sul"
    When eu atualizo o nome da região "Zona Sul" para "Zona Sul Expandida"
    Then a região deve ser atualizada para "Zona Sul Expandida"
