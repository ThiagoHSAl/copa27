package br.ufmg.dcc603.copa27.dominio.modelo;

import br.ufmg.dcc603.copa27.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Grupo (RN01), Classificacao e LinhaClassificacao (RN02). */
class GrupoClassificacaoTest {

    @Test
    @DisplayName("RN01: um grupo tem no máximo quatro seleções")
    void limiteDeQuatro() {
        Grupo g = Fixtures.grupo('A', 1);
        RegraNegocioException e = assertThrows(RegraNegocioException.class,
                () -> g.adicionarSelecao(Fixtures.selecao(9, "Quinta")));
        assertTrue(e.getMessage().contains("quatro"));
        assertEquals(4, g.getSelecoes().size());
    }

    @Test
    @DisplayName("Uma seleção não entra duas vezes no mesmo grupo")
    void semRepeticao() {
        Selecao s = Fixtures.selecao(1, "Brasil");
        Grupo g = new Grupo('B', List.of(s));
        assertThrows(RegraNegocioException.class, () -> g.adicionarSelecao(s));
        assertThrows(RegraNegocioException.class, () -> g.adicionarSelecao(null));
        assertTrue(g.contem(s));
    }

    @Test
    @DisplayName("Grupos vão de A a H e têm identificador numérico")
    void letras() {
        assertThrows(RegraNegocioException.class, () -> new Grupo('I'));
        assertEquals(1, new Grupo('A').getId());
        assertEquals(8, new Grupo('H').getId());
        assertEquals("Grupo C", new Grupo('C').toString());
    }

    @Test
    @DisplayName("CU04, A1: antes dos jogos, a classificação tem estatísticas zeradas")
    void classificacaoInicial() {
        Grupo g = Fixtures.grupo('A', 1);
        Classificacao c = g.getClassificacao();
        assertEquals(4, c.getLinhas().size());
        assertFalse(c.isDefinitiva());
        for (LinhaClassificacao l : c.getLinhas()) {
            assertEquals(0, l.getJogos());
            assertEquals(0, l.pontos());
        }
    }

    @Test
    @DisplayName("RN02: vitória vale 3 pontos, empate 1 e derrota 0")
    void pontuacao() {
        LinhaClassificacao l = new LinhaClassificacao(Fixtures.selecao(1, "Brasil"));
        l.contabilizar(3, 1);
        l.contabilizar(1, 1);
        l.contabilizar(0, 2);
        assertEquals(3, l.getJogos());
        assertEquals(1, l.getVitorias());
        assertEquals(1, l.getEmpates());
        assertEquals(1, l.getDerrotas());
        assertEquals(4, l.pontos());
        assertEquals(4, l.getGolsPro());
        assertEquals(4, l.getGolsContra());
        assertEquals(0, l.saldo());
        assertThrows(RegraNegocioException.class, () -> l.contabilizar(-1, 0));
    }

    @Test
    @DisplayName("Classificação informa posição, primeira e segunda colocadas")
    void posicoes() {
        Selecao a = Fixtures.selecao(1, "A");
        Selecao b = Fixtures.selecao(2, "B");
        Classificacao c = new Classificacao(List.of(new LinhaClassificacao(b), new LinhaClassificacao(a)), true);
        assertSame(b, c.primeira());
        assertSame(a, c.segunda());
        assertEquals(1, c.posicaoDe(b));
        assertEquals(2, c.posicaoDe(a));
        assertEquals(0, c.posicaoDe(Fixtures.selecao(3, "C")));
        assertTrue(c.isDefinitiva());
        Classificacao vazia = new Classificacao(List.of(), false);
        assertNull(vazia.primeira());
        assertNull(vazia.segunda());
    }

    @Test
    @DisplayName("Incluir seleção descarta a classificação antiga")
    void inclusaoReiniciaClassificacao() {
        Grupo g = new Grupo('D');
        g.setClassificacao(new Classificacao(List.of(), true));
        g.adicionarSelecao(Fixtures.selecao(1, "X"));
        assertEquals(1, g.getClassificacao().getLinhas().size());
        assertFalse(g.getClassificacao().isDefinitiva());
    }
}
