package br.com.pinguimice.admin.entity

enum class TipoEntrada {
    CAIXA,   // Box: 480 packets × 4.4 units = 2,112 units
    PACOTE,  // Packet: 1 packet × 4.4 units
    KG       // Kilogram: for coco em pó, maçã verde
}
