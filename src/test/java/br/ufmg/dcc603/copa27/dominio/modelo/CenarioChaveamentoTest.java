package br.ufmg.dcc603.copa27.dominio.modelo;

import br.ufmg.dcc603.copa27.Fixtures;
import br.ufmg.dcc603.copa27.dominio.regras.GeradorChaveamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Cenario (cópia independente, RN07) e Chaveamento (árvore e caminho até a final). */
class CenarioChaveamentoTest {

    private Grupo grupo;
    private List<Partida> partidas;
    private Chaveamento chaveamento;
    private Cenario cenario;

    @BeforeEach
    void preparar() {
        grupo = Fixtures.grupo('A', 1);
        partidas = new ArrayList<>(Fixtures.jogosDoGrupo(grupo, 1));
        List<Partida> mataMata = Fixtures.partidasMataMata();
        partidas.addAll(mataMata);
        chaveamento = new GeradorChaveamento().montarChaveamento(mataMata);
        cenario = new Cenario(List.of(grupo), partidas, chaveamento);
    }

    @Test
    @DisplayName("RN07: simular na cópia não altera o cenário original")
    void copiaIndependente() {
        Cenario copia = cenario.copiar();
        copia.buscarPartida(1).simular(Resultado.placar(3, 0));
        copia.getGrupos().get(0).setClassificacao(new Classificacao(List.of(), true));
        assertNull(cenario.buscarPartida(1).getResultado());
        assertFalse(grupo.getClassificacao().isDefinitiva());
        assertNotSame(cenario.buscarPartida(1), copia.buscarPartida(1));
        assertSame(copia.getGrupos().get(0), copia.buscarPartida(1).getGrupo());
    }

    @Test
    @DisplayName("A cópia preserva situação e resultado e liga o chaveamento às partidas copiadas")
    void copiaPreservaEstado() {
        Fixtures.encerrar(cenario.buscarPartida(1), Resultado.placar(2, 1));
        Cenario copia = cenario.copiar();
        Partida p = copia.buscarPartida(1);
        assertEquals(SituacaoPartida.ENCERRADA, p.getSituacao());
        assertEquals(Resultado.placar(2, 1), p.getResultado());
        Confronto final1 = copia.getChaveamento().buscarConfronto(16);
        assertSame(copia.buscarPartida(64), final1.getPartida());
        assertSame(copia.getChaveamento().buscarConfronto(16), copia.getChaveamento().buscarConfronto(13).getProximo());
        assertEquals(16, copia.getChaveamento().getConfrontos().size());
    }

    @Test
    @DisplayName("Partidas não disputadas excluem as encerradas")
    void naoDisputadas() {
        Fixtures.encerrar(cenario.buscarPartida(1), Resultado.placar(1, 1));
        assertEquals(partidas.size() - 1, cenario.partidasNaoDisputadas().size());
        assertTrue(cenario.partidasNaoDisputadas().stream().noneMatch(p -> p.getId() == 1));
    }

    @Test
    @DisplayName("Busca de partida, seleção, grupo e partidas do grupo")
    void buscas() {
        assertNull(cenario.buscarPartida(999));
        Selecao s = grupo.getSelecoes().get(2);
        assertSame(s, cenario.buscarSelecao(s.getId()));
        assertNull(cenario.buscarSelecao(999));
        assertSame(grupo, cenario.grupoDa(s));
        assertNull(cenario.grupoDa(Fixtures.selecao(99, "Fora")));
        assertEquals(6, cenario.partidasDoGrupo(grupo).size());
    }

    @Test
    @DisplayName("O chaveamento tem 16 confrontos distribuídos nas fases")
    void estrutura() {
        assertEquals(8, chaveamento.confrontosDaFase(Fase.OITAVAS).size());
        assertEquals(4, chaveamento.confrontosDaFase(Fase.QUARTAS).size());
        assertEquals(2, chaveamento.confrontosDaFase(Fase.SEMIFINAL).size());
        assertEquals(1, chaveamento.confrontosDaFase(Fase.TERCEIRO_LUGAR).size());
        assertEquals(1, chaveamento.confrontosDaFase(Fase.FINAL).size());
        Partida p49 = cenario.buscarPartida(49);
        assertEquals(1, chaveamento.confrontoDe(p49).getNumero());
        assertNull(chaveamento.confrontoDe(cenario.buscarPartida(1)));
        assertNull(chaveamento.buscarConfronto(17));
    }

    @Test
    @DisplayName("Confronto exibe a vaga enquanto a seleção não é conhecida")
    void descricaoDaVaga() {
        Confronto c = chaveamento.buscarConfronto(1);
        assertEquals("1º do Grupo A", c.descricaoMandante());
        assertEquals("2º do Grupo C", c.descricaoVisitante());
        Selecao s = grupo.getSelecoes().get(0);
        c.getPartida().definirMandante(s);
        assertEquals(s.getPais(), c.descricaoMandante());
        assertThrows(RegraNegocioException.class, () -> new Confronto(1, cenario.buscarPartida(1), "a", "b"));
    }

    @Test
    @DisplayName("CU08: caminho até a final segue a árvore enquanto a seleção não é eliminada")
    void caminhoAteFinal() {
        Selecao a = grupo.getSelecoes().get(0);
        Selecao b = grupo.getSelecoes().get(1);
        Partida oitava1 = chaveamento.buscarConfronto(1).getPartida();
        oitava1.definirMandante(a);
        oitava1.definirVisitante(b);

        List<Confronto> caminho = chaveamento.caminhoAteFinal(a);
        assertEquals(List.of(1, 9, 13, 16), caminho.stream().map(Confronto::getNumero).toList());

        oitava1.simular(Resultado.placar(0, 1));
        assertEquals(List.of(1), chaveamento.caminhoAteFinal(a).stream().map(Confronto::getNumero).toList());
        assertEquals(4, chaveamento.caminhoAteFinal(b).size());
        assertTrue(chaveamento.caminhoAteFinal(grupo.getSelecoes().get(3)).isEmpty());
    }
}
