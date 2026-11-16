package br.ufmg.dcc603.copa27.dominio.regras;

import br.ufmg.dcc603.copa27.dominio.modelo.Chaveamento;
import br.ufmg.dcc603.copa27.dominio.modelo.Classificacao;
import br.ufmg.dcc603.copa27.dominio.modelo.Confronto;
import br.ufmg.dcc603.copa27.dominio.modelo.Fase;
import br.ufmg.dcc603.copa27.dominio.modelo.Grupo;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.RegraNegocioException;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import br.ufmg.dcc603.copa27.dominio.modelo.SituacaoPartida;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Regras de cruzamento do mata-mata (RN04). Como a FIFA ainda não divulgou o
 * regulamento de 2027, adota-se o cruzamento da Copa Feminina de 2023, que
 * também teve 32 seleções em 8 grupos.
 */
public class GeradorChaveamento {

    /** Oitavas: {letra, posição} da mandante e da visitante de cada confronto, de 1 a 8. */
    private static final Object[][] OITAVAS = {
            {'A', 1, 'C', 2}, {'C', 1, 'A', 2}, {'E', 1, 'G', 2}, {'G', 1, 'E', 2},
            {'B', 1, 'D', 2}, {'D', 1, 'B', 2}, {'F', 1, 'H', 2}, {'H', 1, 'F', 2},
    };

    /** Quartas: números dos confrontos das oitavas cujas vencedoras se enfrentam. */
    private static final int[][] QUARTAS = {{1, 3}, {2, 4}, {5, 7}, {6, 8}};

    /**
     * Monta a árvore vazia de 16 confrontos a partir das 16 partidas de
     * mata-mata: 8 de oitavas, 4 de quartas, 2 semifinais, terceiro lugar e final.
     */
    public Chaveamento montarChaveamento(List<Partida> partidasMataMata) {
        List<Partida> oitavas = daFase(partidasMataMata, Fase.OITAVAS, 8);
        List<Partida> quartas = daFase(partidasMataMata, Fase.QUARTAS, 4);
        List<Partida> semis = daFase(partidasMataMata, Fase.SEMIFINAL, 2);
        Partida terceiro = daFase(partidasMataMata, Fase.TERCEIRO_LUGAR, 1).get(0);
        Partida finalP = daFase(partidasMataMata, Fase.FINAL, 1).get(0);

        List<Confronto> todos = new ArrayList<>();
        List<Confronto> co = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Object[] v = OITAVAS[i];
            co.add(new Confronto(i + 1, oitavas.get(i), vagaGrupo(v[1], v[0]), vagaGrupo(v[3], v[2])));
        }
        List<Confronto> cq = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int a = QUARTAS[i][0];
            int b = QUARTAS[i][1];
            Confronto q = new Confronto(9 + i, quartas.get(i),
                    "Vencedora da oitava " + a, "Vencedora da oitava " + b);
            co.get(a - 1).ligarAo(q, true);
            co.get(b - 1).ligarAo(q, false);
            cq.add(q);
        }
        List<Confronto> cs = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Confronto s = new Confronto(13 + i, semis.get(i),
                    "Vencedora da quarta " + (2 * i + 1), "Vencedora da quarta " + (2 * i + 2));
            cq.get(2 * i).ligarAo(s, true);
            cq.get(2 * i + 1).ligarAo(s, false);
            cs.add(s);
        }
        Confronto ct = new Confronto(15, terceiro, "Perdedora da semifinal 1", "Perdedora da semifinal 2");
        Confronto cf = new Confronto(16, finalP, "Vencedora da semifinal 1", "Vencedora da semifinal 2");
        cs.get(0).ligarAo(cf, true);
        cs.get(1).ligarAo(cf, false);

        todos.addAll(co);
        todos.addAll(cq);
        todos.addAll(cs);
        todos.add(ct);
        todos.add(cf);
        return new Chaveamento(todos);
    }

    private static String vagaGrupo(Object posicao, Object letra) {
        return posicao + "º do Grupo " + letra;
    }

    private static List<Partida> daFase(List<Partida> partidas, Fase f, int quantidade) {
        List<Partida> l = partidas.stream().filter(p -> p.getFase() == f)
                .sorted(Comparator.comparingInt(Partida::getId)).toList();
        if (l.size() != quantidade) {
            throw new RegraNegocioException("São necessárias " + quantidade + " partidas da fase " + f.getDescricao() + ".");
        }
        return l;
    }

    /** A fase de grupos termina quando todas as suas partidas estão encerradas. */
    public boolean faseDeGruposEncerrada(List<Partida> partidas) {
        List<Partida> deGrupos = partidas.stream().filter(p -> p.getFase() == Fase.GRUPOS).toList();
        return !deGrupos.isEmpty() && deGrupos.stream().allMatch(Partida::estaDisputada);
    }

    /**
     * Coloca as duas primeiras de cada grupo nas oitavas (RN04, CU21). Grupos
     * cuja classificação ainda não é definitiva deixam suas vagas indefinidas
     * (CU07, A1).
     */
    public void preencherOitavas(Chaveamento ch, List<Grupo> grupos) {
        for (int i = 0; i < OITAVAS.length; i++) {
            Partida p = ch.buscarConfronto(i + 1).getPartida();
            Selecao m = classificada(grupos, (char) OITAVAS[i][0], (int) OITAVAS[i][1]);
            Selecao v = classificada(grupos, (char) OITAVAS[i][2], (int) OITAVAS[i][3]);
            if (m != null) {
                p.definirMandante(m);
            }
            if (v != null) {
                p.definirVisitante(v);
            }
        }
    }

    private static Selecao classificada(List<Grupo> grupos, char letra, int posicao) {
        Grupo g = grupos.stream().filter(x -> x.getLetra() == letra).findFirst()
                .orElseThrow(() -> new RegraNegocioException("Grupo " + letra + " não encontrado."));
        Classificacao c = g.getClassificacao();
        if (!c.isDefinitiva()) {
            return null;
        }
        return posicao == 1 ? c.primeira() : c.segunda();
    }

    /**
     * Leva a vencedora da partida ao confronto seguinte; na semifinal, leva
     * também a perdedora à disputa de terceiro lugar.
     */
    public void avancarVencedora(Chaveamento ch, Partida p) {
        Confronto c = ch.confrontoDe(p);
        Selecao vencedora = p.vencedora();
        if (c == null || vencedora == null) {
            return;
        }
        Confronto proximo = c.getProximo();
        if (proximo != null) {
            if (c.isVencedoraEntraComoMandante()) {
                proximo.getPartida().definirMandante(vencedora);
            } else {
                proximo.getPartida().definirVisitante(vencedora);
            }
        }
        if (p.getFase() == Fase.SEMIFINAL) {
            Partida terceiro = ch.confrontosDaFase(Fase.TERCEIRO_LUGAR).get(0).getPartida();
            boolean primeiraSemi = ch.confrontosDaFase(Fase.SEMIFINAL).get(0) == c;
            if (primeiraSemi) {
                terceiro.definirMandante(p.perdedora());
            } else {
                terceiro.definirVisitante(p.perdedora());
            }
        }
    }

    /**
     * Recalcula as oitavas e propaga, fase a fase, as vencedoras já conhecidas.
     * Usado na projeção do chaveamento simulado (CU07).
     */
    public void projetar(Chaveamento ch, List<Grupo> grupos) {
        preencherOitavas(ch, grupos);
        for (Fase f : List.of(Fase.OITAVAS, Fase.QUARTAS, Fase.SEMIFINAL)) {
            for (Confronto c : ch.confrontosDaFase(f)) {
                if (c.getPartida().vencedora() != null) {
                    avancarVencedora(ch, c.getPartida());
                } else {
                    liberarVagasSeguintes(ch, c);
                }
            }
        }
    }

    /**
     * Sem vencedora definida, a vaga no confronto seguinte volta a ficar em
     * aberto; isso descarta projeções que dependiam de um placar alterado.
     */
    private void liberarVagasSeguintes(Chaveamento ch, Confronto c) {
        Confronto proximo = c.getProximo();
        if (proximo != null && proximo.getPartida().getSituacao() == SituacaoPartida.AGENDADA) {
            if (c.isVencedoraEntraComoMandante()) {
                proximo.getPartida().definirMandante(null);
            } else {
                proximo.getPartida().definirVisitante(null);
            }
        }
        if (c.getFase() == Fase.SEMIFINAL) {
            Partida terceiro = ch.confrontosDaFase(Fase.TERCEIRO_LUGAR).get(0).getPartida();
            if (terceiro.getSituacao() == SituacaoPartida.AGENDADA) {
                if (ch.confrontosDaFase(Fase.SEMIFINAL).get(0) == c) {
                    terceiro.definirMandante(null);
                } else {
                    terceiro.definirVisitante(null);
                }
            }
        }
    }
}
