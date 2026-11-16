package br.ufmg.dcc603.copa27.dominio.modelo;

/**
 * Hotel próximo a uma sede. Faz parte do modelo (Documento 05), mas sua
 * consulta (RF17 e RF18, Should e Could) não integra a implementação parcial.
 */
public record Hotel(String nome, int categoria, FaixaPreco faixaPreco, double distanciaKm) {
}
