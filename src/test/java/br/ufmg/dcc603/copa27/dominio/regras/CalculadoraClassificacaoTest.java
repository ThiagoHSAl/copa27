package br.ufmg.dcc603.copa27.dominio.regras;

import br.ufmg.dcc603.copa27.Fixtures;
import br.ufmg.dcc603.copa27.dominio.modelo.Classificacao;
import br.ufmg.dcc603.copa27.dominio.modelo.Grupo;
import br.ufmg.dcc603.copa27.dominio.modelo.LinhaClassificacao;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.Resultado;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** CU20: cálculo da classificação a partir dos resultados registrados. */
class CalculadoraClassificacaoTest {

    private final CalculadoraClassificacao calculadora = new CalculadoraClassificacao();
    private Grupo grupo;
    private List<Selecao> s;
    private List<Partida> jogos;

    @BeforeEach
    void preparar() {
        grupo = Fixtures.grupo('A', 1);
        s = grupo.getSelecoes();
        // Ordem dos jogos: s0 x s1, s2 x s3, s0 x s2, s3 x s1, s3 x s0, s1 x s2.
        jogos = Fixtures.jogosDoGrupo(grupo, 1);
    }

    private void encerrarTodos(int[][] placares) {
        for (int i = 0; i < jogos.size(); i++) {
            Fixtures.encerrar(jogos.get(i), Resultado.placar(placares[i][0], placares[i][1]));
        }
    }

    @Test
    @DisplayName("Sem resultados, todas as linhas ficam zeradas e a classificação não é definitiva")
    void semResultados() {
        Classificacao c = calculadora.calcular(grupo, jogos);
        assertEquals(4, c.getLinhas().size());
        assertTrue(c.getLinhas().stream().allMatch(l -> l.getJogos() == 0));
        assertFalse(c.isDefinitiva());
    }

    @Test
    @DisplayName("Calcula estatísticas e ordena pelos critérios oficiais")
    void grupoCompleto() {
        // s0: V 2x0, V 1x0, V 3x1 -> 9 pts; s1: D, V 0x2 (como visitante vence 2x0), E 1x1
        encerrarTodos(new int[][]{{2, 0}, {1, 1}, {1, 0}, {0, 2}, {1, 3}, {1, 1}});
        Classificacao c = calculadora.calcular(grupo, jogos);
        LinhaClassificacao primeira = c.getLinhas().get(0);
        assertSame(s.get(0), c.primeira());
        assertEquals(9, primeira.pontos());
        assertEquals(3, primeira.getVitorias());
        assertEquals(6, primeira.getGolsPro());
        assertEquals(1, primeira.getGolsContra());
        assertSame(s.get(1), c.segunda());
        assertEquals(4, c.getLinhas().get(1).pontos());
        assertTrue(c.isDefinitiva());
        int totalJogos = c.getLinhas().stream().mapToInt(LinhaClassificacao::getJogos).sum();
        assertEquals(12, totalJogos);
    }

    @Test
    @DisplayName("Resultado parcial de partida em andamento entra na tabela, mas não a torna definitiva")
    void parcialEmAndamento() {
        Partida p = jogos.get(0);
        p.iniciar();
        p.registrarResultado(Resultado.placar(1, 0));
        Classificacao c = calculadora.calcular(grupo, jogos);
        assertSame(s.get(0), c.primeira());
        assertEquals(3, c.getLinhas().get(0).pontos());
        assertFalse(c.isDefinitiva());
    }

    @Test
    @DisplayName("Placares simulados tornam a classificação definitiva no cenário simulado")
    void simuladaDefinitiva() {
        jogos.forEach(p -> p.simular(Resultado.placar(0, 1)));
        assertTrue(calculadora.calcular(grupo, jogos).isDefinitiva());
    }

    @Test
    @DisplayName("Ignora partidas de outros grupos")
    void ignoraOutrosGrupos() {
        Grupo outro = Fixtures.grupo('B', 11);
        List<Partida> todas = new ArrayList<>(jogos);
        List<Partida> jogosB = Fixtures.jogosDoGrupo(outro, 7);
        Fixtures.encerrar(jogosB.get(0), Resultado.placar(4, 0));
        todas.addAll(jogosB);
        Classificacao c = calculadora.calcular(grupo, todas);
        assertTrue(c.getLinhas().stream().allMatch(l -> l.getJogos() == 0));
    }

    @Test
    @DisplayName("Confronto direto desempata seleções iguais em pontos, saldo e gols pró")
    void desempatePorConfrontoDireto() {
        // Jogos: s0 0x0 s1, s2 1x0 s3, s0 1x0 s2, s3 2x0 s1, s3 1x0 s0, s1 0x2 s2.
        // s2 e s3 terminam com 6 pontos, saldo 2 e 3 gols pró; s2 venceu o confronto direto.
        // s0 tem 4 pontos e s1, 1 ponto.
        encerrarTodos(new int[][]{{0, 0}, {1, 0}, {1, 0}, {2, 0}, {1, 0}, {0, 2}});
        Classificacao c = calculadora.calcular(grupo, jogos);
        LinhaClassificacao l2 = c.getLinhas().get(0);
        LinhaClassificacao l3 = c.getLinhas().get(1);
        assertEquals(l2.pontos(), l3.pontos());
        assertEquals(l2.saldo(), l3.saldo());
        assertEquals(l2.getGolsPro(), l3.getGolsPro());
        assertEquals(List.of(s.get(2), s.get(3), s.get(0), s.get(1)),
                c.getLinhas().stream().map(LinhaClassificacao::getSelecao).toList());
    }
}
