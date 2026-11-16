package br.ufmg.dcc603.copa27.dominio.modelo;

import java.util.Objects;

/** Aeroporto próximo a uma sede (RF16). */
public record Aeroporto(String nome, String codigoIata, double distanciaKm) {

    public Aeroporto {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O nome do aeroporto é obrigatório.");
        }
        Objects.requireNonNull(codigoIata);
        if (!codigoIata.matches("[A-Z]{3}")) {
            throw new RegraNegocioException("O código IATA deve ter três letras maiúsculas.");
        }
        if (distanciaKm < 0) {
            throw new RegraNegocioException("A distância até o estádio não pode ser negativa.");
        }
    }
}
