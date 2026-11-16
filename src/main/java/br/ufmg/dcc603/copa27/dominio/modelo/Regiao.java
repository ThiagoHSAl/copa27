package br.ufmg.dcc603.copa27.dominio.modelo;

/** Regiões do Brasil. */
public enum Regiao {
    NORTE("Norte"),
    NORDESTE("Nordeste"),
    CENTRO_OESTE("Centro-Oeste"),
    SUDESTE("Sudeste"),
    SUL("Sul");

    private final String descricao;

    Regiao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
