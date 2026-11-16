package br.ufmg.dcc603.copa27;

import br.ufmg.dcc603.copa27.dominio.modelo.Aeroporto;
import br.ufmg.dcc603.copa27.dominio.modelo.Confederacao;
import br.ufmg.dcc603.copa27.dominio.modelo.Estadio;
import br.ufmg.dcc603.copa27.dominio.modelo.Fase;
import br.ufmg.dcc603.copa27.dominio.modelo.Grupo;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.Regiao;
import br.ufmg.dcc603.copa27.dominio.modelo.Resultado;
import br.ufmg.dcc603.copa27.dominio.modelo.Sede;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Construtores de objetos de domínio para os testes. */
public final class Fixtures {

    public static final LocalDateTime DATA = LocalDateTime.of(2027, 6, 24, 16, 0);

    private Fixtures() {
    }

    public static Estadio estadio() {
        return new Estadio("Estádio Teste", 50_000, 2000, "Rua dos Testes, 1");
    }

    public static Sede sede(int id, String cidade) {
        return new Sede(id, cidade, Regiao.SUDESTE, estadio(),
                List.of(new Aeroporto("Aeroporto Teste", "TST", 10.0)));
    }

    public static Selecao selecao(int id, String pais) {
        String codigo = (pais.replaceAll("[^A-Za-z]", "").toUpperCase() + "XXX").substring(0, 3);
        return new Selecao(id, pais, codigo, Confederacao.UEFA);
    }

    /** Grupo com quatro seleções de identificadores consecutivos. */
    public static Grupo grupo(char letra, int primeiroId) {
        Grupo g = new Grupo(letra);
        for (int i = 0; i < 4; i++) {
            g.adicionarSelecao(selecao(primeiroId + i, "Pais" + letra + (char) ('a' + i)));
        }
        return g;
    }

    public static Partida jogoGrupo(int id, Grupo g, Selecao m, Selecao v) {
        return new Partida(id, Fase.GRUPOS, DATA.plusDays(id), estadio(), g, m, v);
    }

    /** Os seis jogos de um grupo em turno único: 1x2, 3x4, 1x3, 4x2, 4x1, 2x3. */
    public static List<Partida> jogosDoGrupo(Grupo g, int primeiroId) {
        List<Selecao> s = g.getSelecoes();
        int[][] pares = {{0, 1}, {2, 3}, {0, 2}, {3, 1}, {3, 0}, {1, 2}};
        List<Partida> l = new ArrayList<>();
        for (int i = 0; i < pares.length; i++) {
            l.add(jogoGrupo(primeiroId + i, g, s.get(pares[i][0]), s.get(pares[i][1])));
        }
        return l;
    }

    public static Partida jogoMataMata(int id, Fase f, Selecao m, Selecao v) {
        return new Partida(id, f, DATA.plusDays(20), estadio(), null, m, v);
    }

    /** As 16 partidas de mata-mata, de 49 a 64, sem seleções. */
    public static List<Partida> partidasMataMata() {
        List<Partida> l = new ArrayList<>();
        int id = 49;
        for (int i = 0; i < 8; i++) {
            l.add(jogoMataMata(id++, Fase.OITAVAS, null, null));
        }
        for (int i = 0; i < 4; i++) {
            l.add(jogoMataMata(id++, Fase.QUARTAS, null, null));
        }
        l.add(jogoMataMata(id++, Fase.SEMIFINAL, null, null));
        l.add(jogoMataMata(id++, Fase.SEMIFINAL, null, null));
        l.add(jogoMataMata(id++, Fase.TERCEIRO_LUGAR, null, null));
        l.add(jogoMataMata(id, Fase.FINAL, null, null));
        return l;
    }

    /** Leva a partida até o fim, pelo fluxo real: iniciar, registrar e encerrar. */
    public static Partida encerrar(Partida p, Resultado r) {
        p.iniciar();
        p.registrarResultado(r);
        p.encerrar();
        return p;
    }
}
