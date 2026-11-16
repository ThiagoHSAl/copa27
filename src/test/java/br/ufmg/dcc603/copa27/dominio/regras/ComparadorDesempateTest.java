package br.ufmg.dcc603.copa27.dominio.regras;

import br.ufmg.dcc603.copa27.Fixtures;
import br.ufmg.dcc603.copa27.dominio.modelo.Grupo;
import br.ufmg.dcc603.copa27.dominio.modelo.LinhaClassificacao;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.Resultado;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** RN03: pontos, saldo de gols, gols pró e confronto direto, nesta ordem. */
class ComparadorDesempateTest {

    private Grupo grupo;
    private Selecao a;
    private Selecao b;
    private LinhaClassificacao la;
    private LinhaClassificacao lb;

    @BeforeEach
    void preparar() {
        grupo = Fixtures.grupo('A', 1);
        a = grupo.getSelecoes().get(0);
        b = grupo.getSelecoes().get(1);
        la = new LinhaClassificacao(a);
        lb = new LinhaClassificacao(b);
    }

    private static ComparadorDesempate semPartidas() {
        return new ComparadorDesempate(List.of());
    }

    @Test
    @DisplayName("Mais pontos fica à frente")
    void pontos() {
        la.contabilizar(1, 0);
        lb.contabilizar(5, 5);
        assertTrue(semPartidas().compare(la, lb) < 0);
        assertTrue(semPartidas().compare(lb, la) > 0);
    }

    @Test
    @DisplayName("Com pontos iguais, decide o saldo de gols")
    void saldo() {
        la.contabilizar(3, 0);
        lb.contabilizar(1, 0);
        assertTrue(semPartidas().compare(la, lb) < 0);
    }

    @Test
    @DisplayName("Com pontos e saldo iguais, decidem os gols pró")
    void golsPro() {
        la.contabilizar(1, 1);
        lb.contabilizar(3, 3);
        assertTrue(semPartidas().compare(lb, la) < 0);
    }

    @Test
    @DisplayName("Com pontos, saldo e gols pró iguais, decide o confronto direto")
    void confrontoDireto() {
        // Cada uma tem 3 pontos, 2 gols pró e 2 contra; no confronto direto, b venceu a.
        la.contabilizar(1, 2);
        la.contabilizar(1, 0);
        lb.contabilizar(2, 1);
        lb.contabilizar(0, 1);
        Partida direto = Fixtures.encerrar(Fixtures.jogoGrupo(1, grupo, a, b), Resultado.placar(1, 2));
        ComparadorDesempate c = new ComparadorDesempate(List.of(direto));
        assertTrue(c.compare(lb, la) < 0);
        assertTrue(c.compare(la, lb) > 0);
    }

    @Test
    @DisplayName("Empate em todos os critérios usa a ordem alfabética, de forma determinística")
    void ordemAlfabetica() {
        Partida empate = Fixtures.encerrar(Fixtures.jogoGrupo(1, grupo, a, b), Resultado.placar(1, 1));
        la.contabilizar(1, 1);
        lb.contabilizar(1, 1);
        ComparadorDesempate c = new ComparadorDesempate(List.of(empate));
        assertTrue(c.compare(la, lb) < 0, "PaisAa vem antes de PaisAb");
        assertEquals(0, c.compare(la, la));
    }

    @Test
    @DisplayName("Partidas sem resultado ou de outras seleções não contam no confronto direto")
    void ignoraPartidasIrrelevantes() {
        Selecao c = grupo.getSelecoes().get(2);
        Partida semResultado = Fixtures.jogoGrupo(1, grupo, a, b);
        Partida outra = Fixtures.encerrar(Fixtures.jogoGrupo(2, grupo, a, c), Resultado.placar(5, 0));
        ComparadorDesempate comp = new ComparadorDesempate(List.of(semResultado, outra));
        assertTrue(comp.compare(la, lb) < 0, "sem confronto direto, cai na ordem alfabética");
    }
}
