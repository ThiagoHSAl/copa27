package br.ufmg.dcc603.copa27.servico;

import br.ufmg.dcc603.copa27.Ambiente;
import br.ufmg.dcc603.copa27.dominio.modelo.Cenario;
import br.ufmg.dcc603.copa27.dominio.modelo.Confronto;
import br.ufmg.dcc603.copa27.dominio.modelo.Fase;
import br.ufmg.dcc603.copa27.dominio.modelo.Partida;
import br.ufmg.dcc603.copa27.dominio.modelo.RegraNegocioException;
import br.ufmg.dcc603.copa27.dominio.modelo.Resultado;
import br.ufmg.dcc603.copa27.dominio.modelo.Selecao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** CU06 a CU09 e a regra RN07. */
class ServicoSimulacaoTest {

    private Ambiente amb;
    private ServicoSimulacao sim;

    @BeforeEach
    void preparar() {
        amb = new Ambiente();
        sim = amb.simulacao;
    }

    /** Simula todas as partidas de grupos não disputadas com vitória da mandante por 2 x 0. */
    private void simularGrupos(Cenario c) {
        for (Partida p : c.partidasNaoDisputadas()) {
            if (p.getFase() == Fase.GRUPOS) {
                sim.simular(c, p.getId(), Resultado.placar(2, 0));
            }
        }
    }

    @Test
    @DisplayName("CU06: iniciar cria um cenário com todas as partidas não disputadas")
    void iniciar() {
        Cenario c = sim.iniciar();
        assertEquals(64, c.partidasNaoDisputadas().size());
        assertEquals(8, c.getGrupos().size());
    }

    @Test
    @DisplayName("RN07: simular não altera os dados reais nem outras simulações")
    void naoAlteraReal() {
        Cenario c1 = sim.iniciar();
        Cenario c2 = sim.iniciar();
        sim.simular(c1, 1, Resultado.placar(5, 0));
        assertNull(amb.consulta.buscarPartida(1).getResultado());
        assertNull(c2.buscarPartida(1).getResultado());
        assertEquals(5, c1.buscarPartida(1).getResultado().getGolsMandante());
        assertEquals(0, amb.competicao.obterClassificacoes().get(0).getLinhas().get(0).getJogos());
        assertEquals(1, c1.getGrupos().get(0).getClassificacao().getLinhas().get(0).getJogos());
        assertTrue(amb.auditoria.listar().isEmpty());
    }

    @Test
    @DisplayName("CU07: simulada a fase de grupos, as oitavas são projetadas")
    void projetaOitavas() {
        Cenario c = sim.iniciar();
        simularGrupos(c);
        for (Confronto cf : c.getChaveamento().confrontosDaFase(Fase.OITAVAS)) {
            assertNotNull(cf.getPartida().getMandante());
            assertNotNull(cf.getPartida().getVisitante());
        }
        Selecao primeiraA = c.getGrupos().get(0).getClassificacao().primeira();
        assertSame(primeiraA, c.getChaveamento().buscarConfronto(1).getPartida().getMandante());
        assertNull(amb.competicao.obterChaveamento().buscarConfronto(1).getPartida().getMandante());
    }

    @Test
    @DisplayName("CU07, A1: grupo incompleto deixa suas vagas indefinidas")
    void grupoIncompleto() {
        Cenario c = sim.iniciar();
        simularGrupos(c);
        Cenario c2 = sim.iniciar();
        for (Partida p : c2.partidasNaoDisputadas()) {
            if (p.getFase() == Fase.GRUPOS && p.getGrupo().getLetra() != 'A') {
                sim.simular(c2, p.getId(), Resultado.placar(1, 0));
            }
        }
        assertNull(c2.getChaveamento().buscarConfronto(1).getPartida().getMandante());
        assertNotNull(c2.getChaveamento().buscarConfronto(1).getPartida().getVisitante());
    }

    @Test
    @DisplayName("CU08: caminho de uma seleção até a final e eliminação no mata-mata")
    void caminho() {
        Cenario c = sim.iniciar();
        simularGrupos(c);
        Partida oitava1 = c.getChaveamento().buscarConfronto(1).getPartida();
        Selecao mandante = oitava1.getMandante();
        Selecao visitante = oitava1.getVisitante();
        assertEquals(4, sim.caminhoAteFinal(c, mandante.getId()).size());
        sim.simular(c, oitava1.getId(), Resultado.comPenaltis(1, 2, 4));
        List<Confronto> caminhoEliminada = sim.caminhoAteFinal(c, mandante.getId());
        assertEquals(1, caminhoEliminada.size());
        assertSame(visitante, c.getChaveamento().buscarConfronto(9).getPartida().getMandante());
        assertEquals(4, sim.caminhoAteFinal(c, visitante.getId()).size());
        assertThrows(NaoEncontradoException.class, () -> sim.caminhoAteFinal(c, 999));
    }

    @Test
    @DisplayName("CU08, A1: seleção não classificada tem caminho vazio")
    void naoClassificada() {
        Cenario c = sim.iniciar();
        simularGrupos(c);
        Selecao ultimaA = c.getGrupos().get(0).getClassificacao().getLinhas().get(3).getSelecao();
        assertTrue(sim.caminhoAteFinal(c, ultimaA.getId()).isEmpty());
    }

    @Test
    @DisplayName("Simula o torneio inteiro até a campeã em menos de 1 segundo (RNF02)")
    void torneioCompletoRapido() {
        long inicio = System.nanoTime();
        Cenario c = sim.iniciar();
        simularGrupos(c);
        for (Fase f : List.of(Fase.OITAVAS, Fase.QUARTAS, Fase.SEMIFINAL, Fase.TERCEIRO_LUGAR, Fase.FINAL)) {
            for (Confronto cf : c.getChaveamento().confrontosDaFase(f)) {
                sim.simular(c, cf.getPartida().getId(), Resultado.comProrrogacao(2, 1));
            }
        }
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        assertNotNull(c.getChaveamento().buscarConfronto(16).getPartida().vencedora());
        assertNotNull(c.getChaveamento().buscarConfronto(15).getPartida().vencedora());
        assertTrue(ms < 1000, "levou " + ms + " ms");
    }

    @Test
    @DisplayName("CU06, E2 e A2: partida encerrada e empate no mata-mata são recusados")
    void recusas() {
        amb.jogar(1, Resultado.placar(1, 0));
        Cenario c = sim.iniciar();
        assertThrows(RegraNegocioException.class, () -> sim.simular(c, 1, Resultado.placar(0, 1)));
        simularGrupos(c);
        int oitava = c.getChaveamento().buscarConfronto(1).getPartida().getId();
        assertThrows(RegraNegocioException.class, () -> sim.simular(c, oitava, Resultado.placar(1, 1)));
        assertThrows(NaoEncontradoException.class, () -> sim.simular(c, 999, Resultado.placar(1, 0)));
        int quartas = c.getChaveamento().buscarConfronto(9).getPartida().getId();
        assertThrows(RegraNegocioException.class, () -> sim.simular(c, quartas, Resultado.placar(1, 0)));
    }

    @Test
    @DisplayName("A simulação parte dos resultados reais já registrados")
    void parteDoReal() {
        amb.jogar(1, Resultado.placar(4, 0));
        Cenario c = sim.iniciar();
        assertEquals(63, c.partidasNaoDisputadas().size());
        assertEquals(3, c.getGrupos().get(0).getClassificacao().getLinhas().get(0).pontos());
    }

    @Test
    @DisplayName("CU09: restaurar descarta a simulação e volta ao cenário real corrente")
    void restaurar() {
        Cenario c = sim.iniciar();
        sim.simular(c, 2, Resultado.placar(1, 1));
        amb.jogar(3, Resultado.placar(0, 2));
        Cenario restaurado = sim.restaurar();
        assertNull(restaurado.buscarPartida(2).getResultado());
        assertTrue(restaurado.buscarPartida(3).estaDisputada());
        assertNotSame(c, restaurado);
    }
}
