package br.ufmg.dcc603.copa27.dominio.modelo;

import java.util.Objects;

/** Desempenho de uma seleção na tabela do grupo (RF05). */
public class LinhaClassificacao {

    public static final int PONTOS_VITORIA = 3;
    public static final int PONTOS_EMPATE = 1;

    private final Selecao selecao;
    private int jogos;
    private int vitorias;
    private int empates;
    private int derrotas;
    private int golsPro;
    private int golsContra;

    public LinhaClassificacao(Selecao selecao) {
        this.selecao = Objects.requireNonNull(selecao);
    }

    /** Pontuação: vitória vale 3, empate 1 e derrota 0 (RN02). */
    public int pontos() {
        return PONTOS_VITORIA * vitorias + PONTOS_EMPATE * empates;
    }

    public int saldo() {
        return golsPro - golsContra;
    }

    /** Soma um jogo com o placar visto do lado desta seleção. */
    public void contabilizar(int golsPro, int golsContra) {
        if (golsPro < 0 || golsContra < 0) {
            throw new RegraNegocioException("Gols não podem ser negativos.");
        }
        jogos++;
        this.golsPro += golsPro;
        this.golsContra += golsContra;
        if (golsPro > golsContra) {
            vitorias++;
        } else if (golsPro == golsContra) {
            empates++;
        } else {
            derrotas++;
        }
    }

    LinhaClassificacao copiar() {
        LinhaClassificacao c = new LinhaClassificacao(selecao);
        c.jogos = jogos;
        c.vitorias = vitorias;
        c.empates = empates;
        c.derrotas = derrotas;
        c.golsPro = golsPro;
        c.golsContra = golsContra;
        return c;
    }

    public Selecao getSelecao() {
        return selecao;
    }

    public int getJogos() {
        return jogos;
    }

    public int getVitorias() {
        return vitorias;
    }

    public int getEmpates() {
        return empates;
    }

    public int getDerrotas() {
        return derrotas;
    }

    public int getGolsPro() {
        return golsPro;
    }

    public int getGolsContra() {
        return golsContra;
    }
}
