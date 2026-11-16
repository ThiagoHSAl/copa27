package br.ufmg.dcc603.copa27.dominio.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Tabela de um grupo, com as linhas em ordem de classificação. É
 * definitiva quando todas as partidas do grupo têm resultado definitivo.
 */
public class Classificacao {

    private final List<LinhaClassificacao> linhas;
    private final boolean definitiva;

    public Classificacao(List<LinhaClassificacao> linhasOrdenadas, boolean definitiva) {
        this.linhas = new ArrayList<>(linhasOrdenadas);
        this.definitiva = definitiva;
    }

    /** Classificação antes de qualquer jogo: estatísticas zeradas (CU04, A1). */
    public static Classificacao inicial(List<Selecao> selecoes) {
        return new Classificacao(selecoes.stream().map(LinhaClassificacao::new).toList(), false);
    }

    /** Posição da seleção, a partir de 1, ou 0 se ela não estiver no grupo. */
    public int posicaoDe(Selecao s) {
        for (int i = 0; i < linhas.size(); i++) {
            if (linhas.get(i).getSelecao() == s) {
                return i + 1;
            }
        }
        return 0;
    }

    public Selecao primeira() {
        return linhas.isEmpty() ? null : linhas.get(0).getSelecao();
    }

    public Selecao segunda() {
        return linhas.size() < 2 ? null : linhas.get(1).getSelecao();
    }

    public List<LinhaClassificacao> getLinhas() {
        return List.copyOf(linhas);
    }

    public boolean isDefinitiva() {
        return definitiva;
    }

    Classificacao copiar() {
        return new Classificacao(linhas.stream().map(LinhaClassificacao::copiar).toList(), definitiva);
    }
}
