Feature: Gestão de Despesas

  Background:
    Given que eu sou um usuário autenticado

  Scenario: Criar despesa sem anexo
    When eu crio uma despesa "Conta de Luz" com valor 150.00
    Then a despesa "Conta de Luz" deve ser listada com sucesso

  Scenario: Criar despesa com anexo (Simulado)
    When eu crio uma despesa "Recibo" com valor 50.00 e anexo "recibo.pdf"
    Then a despesa "Recibo" deve ter um link de anexo
