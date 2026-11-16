package br.ufmg.dcc603.copa27.dominio.regras;

import br.ufmg.dcc603.copa27.dominio.modelo.LinhaClassificacao;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.Resultado;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;

import java.util.Comparator;
import java.util.List;

/**
 * Ordena as linhas de um grupo pelos critérios oficiais (RN03): pontos,
 * saldo de gols, gols pró e confronto direto. Quem fica à frente é
 * "menor" na ordenação. Persistindo o empate, usa a ordem alfabética do país
 * como critério final determinístico, no lugar do sorteio do regulamento.
 */
public class ComparadorDesempate implements Comparator<LinhaClassificacao> {

    private final List<Partida> partidasDoGrupo;

    public ComparadorDesempate(List<Partida> partidasDoGrupo) {
        this.partidasDoGrupo = List.copyOf(partidasDoGrupo);
    }

    @Override
    public int compare(LinhaClassificacao a, LinhaClassificacao b) {
        int r = Integer.compare(b.pontos(), a.pontos());
        if (r != 0) {
            return r;
        }
        r = Integer.compare(b.saldo(), a.saldo());
        if (r != 0) {
            return r;
        }
        r = Integer.compare(b.getGolsPro(), a.getGolsPro());
        if (r != 0) {
            return r;
        }
        r = confrontoDireto(a.getSelecao(), b.getSelecao());
        if (r != 0) {
            return r;
        }
        return a.getSelecao().getPais().compareTo(b.getSelecao().getPais());
    }

    /** Compara pontos e, depois, saldo nos jogos entre as duas seleções. */
    private int confrontoDireto(Selecao a, Selecao b) {
        LinhaClassificacao linhaA = new LinhaClassificacao(a);
        LinhaClassificacao linhaB = new LinhaClassificacao(b);
        for (Partida p : partidasDoGrupo) {
            Resultado res = p.getResultado();
            if (res == null || !p.envolve(a) || !p.envolve(b)) {
                continue;
            }
            boolean aMandante = p.getMandante() == a;
            int golsA = aMandante ? res.getGolsMandante() : res.getGolsVisitante();
            int golsB = aMandante ? res.getGolsVisitante() : res.getGolsMandante();
            linhaA.contabilizar(golsA, golsB);
            linhaB.contabilizar(golsB, golsA);
        }
        int r = Integer.compare(linhaB.pontos(), linhaA.pontos());
        return r != 0 ? r : Integer.compare(linhaB.saldo(), linhaA.saldo());
    }
}
