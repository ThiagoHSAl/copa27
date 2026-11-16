package br.ufmg.dcc603.copa27.dominio.modelo;

/** Fases do torneio, na ordem em que são disputadas. */
public enum Fase {
    GRUPOS("Fase de grupos"),
    OITAVAS("Oitavas de final"),
    QUARTAS("Quartas de final"),
    SEMIFINAL("Semifinal"),
    TERCEIRO_LUGAR("Disputa de terceiro lugar"),
    FINAL("Final");

    private final String descricao;

    Fase(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Indica se a fase pertence ao mata-mata, em que não há empate (RN05). */
    public boolean isMataMata() {
        return this != GRUPOS;
    }
}
