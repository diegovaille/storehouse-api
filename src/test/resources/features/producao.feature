Feature: Produção e Dedução de Estoque FIFO

  Background:
    Given que existe um usuário "admin" com email "admin@pinguimice.com.br" e senha "123456"
    And que existe uma organização "Pinguim Ice" com CNPJ "60774613000108"
    And que o usuário "admin" pertence à organização "Pinguim Ice" com perfil "ADMIN"
    And que existe uma filial "Matriz" vinculada à organização "Pinguim Ice"
    And que eu estou autenticado como "admin@pinguimice.com.br"
    And que existe um sabor "Morango"
    And que existe estoque de matéria prima para "Morango" criado ontem com 100 unidades
    And que existe estoque de matéria prima para "Morango" criado hoje com 100 unidades
    And que existe estoque de embalagem para "Morango" com 500 unidades
    And que existe estoque de "Saco Transparente" suficiente
    And que existe um sabor "Coco" que usa açúcar
    And que existe um sabor "Maçã Verde" que usa açúcar
    And que existe estoque de matéria prima para "Coco" criado hoje com 500 unidades
    And que existe estoque de matéria prima para "Maçã Verde" criado hoje com 500 unidades
    And que existe estoque de embalagem para "Coco" com 500 unidades
    And que existe estoque de embalagem para "Maçã Verde" com 500 unidades

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

  # ---------- Casos relacionados a açúcar (MP global) ----------

  Scenario: Produção usando açúcar como insumo (Coco)
    Given que existe estoque de açúcar com 444 unidades totais
    When eu registro uma produção de 444 gelinhos de "Coco" com dedução de estoque
    Then o estoque de açúcar deve ser 0
    And o estoque de gelinho de "Coco" deve aumentar em 444

  Scenario: Produção usando açúcar e reverter dedução (Maçã Verde)
    Given que existe estoque de açúcar com 666 unidades totais
    When eu registro uma produção de 222 gelinhos de "Maçã Verde" com dedução de estoque
    Then o estoque de açúcar deve ser 444
    When eu excluo a última produção registrada
    Then o estoque de açúcar deve ser 666
    And o estoque de gelinho de "Maçã Verde" deve ser 0
