package br.ufmg.dcc603.copa27.dominio.modelo;

/** Estados do ciclo de vida de uma partida (Documento 05, seção 4). */
public enum SituacaoPartida {
    AGENDADA("Agendada"),
    EM_ANDAMENTO("Em andamento"),
    ENCERRADA("Encerrada");

    private final String descricao;

    SituacaoPartida(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
