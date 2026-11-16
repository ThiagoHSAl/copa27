package br.ufmg.dcc603.copa27.dominio.modelo;

/** Estádio de uma sede (RF15). Cada partida é disputada em um estádio. */
public class Estadio {

    private String nome;
    private int capacidade;
    private int anoInauguracao;
    private String endereco;
    private Sede sede;

    public Estadio(String nome, int capacidade, int anoInauguracao, String endereco) {
        validar(nome, capacidade, anoInauguracao, endereco);
        this.nome = nome.trim();
        this.capacidade = capacidade;
        this.anoInauguracao = anoInauguracao;
        this.endereco = endereco.trim();
    }

    private static void validar(String nome, int capacidade, int anoInauguracao, String endereco) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O nome do estádio é obrigatório.");
        }
        if (capacidade <= 0) {
            throw new RegraNegocioException("A capacidade do estádio deve ser positiva.");
        }
        if (anoInauguracao < 1800 || anoInauguracao > 2027) {
            throw new RegraNegocioException("Ano de inauguração inválido.");
        }
        if (endereco == null || endereco.isBlank()) {
            throw new RegraNegocioException("O endereço do estádio é obrigatório.");
        }
    }

    /** Copia os dados de outro estádio mantendo a identidade deste objeto. */
    void atualizarCom(Estadio outro) {
        this.nome = outro.nome;
        this.capacidade = outro.capacidade;
        this.anoInauguracao = outro.anoInauguracao;
        this.endereco = outro.endereco;
    }

    Estadio copiar() {
        return new Estadio(nome, capacidade, anoInauguracao, endereco);
    }

    public String getNome() {
        return nome;
    }

    public int getCapacidade() {
        return capacidade;
    }

    public int getAnoInauguracao() {
        return anoInauguracao;
    }

    public String getEndereco() {
        return endereco;
    }

    /** Sede à qual o estádio pertence; permite listar as partidas de uma sede (RF19). */
    public Sede getSede() {
        return sede;
    }

    void setSede(Sede sede) {
        this.sede = sede;
    }

    @Override
    public String toString() {
        return nome;
    }
}
